/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.authdb.MyPrincipal;
import nl.uva.cs.lobcder.authdb.Permissions;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.webDav.resources.WebDataDirResource;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
/**
 *
 * @author dvasunin
 */
public class JDBCatalogue {

    public Connection getConnection() throws CatalogueException {
        try{
            String jndiName = "jdbc/lobcder";// !!!!!!!!!!!!!!!!!!!!!!!!!!!
            Context ctx = new InitialContext();
            if(ctx == null )
                throw new Exception("JNDI could not create InitalContext ");
            Context envContext  = (Context)ctx.lookup("java:/comp/env");
            DataSource datasource =  (DataSource)envContext.lookup(jndiName);
            return datasource.getConnection();
      } catch(Exception e) {
          throw new CatalogueException(e.getMessage());
      }
    }


    public JDBCatalogue() {
        TimerTask gcTask = new TimerTask() {

            Runnable sweep = deleteSweep();

            @Override
            public void run() {
                sweep.run();
            }
        };
        new Timer(true).schedule(gcTask, 10000, 10000); //once in 10 sec
    }

    public ILogicalData registerPdriForNewEntry(ILogicalData logicalData, PDRI pdri, Connection connection) throws CatalogueException {
        boolean connectionIsProvided;
        boolean connectionAutocommit = false;
        Statement s = null;
        if (connection == null) {
            connectionIsProvided = false;
            connection = getConnection();
        } else {
            connectionIsProvided = true;
        }
        try {
            connectionAutocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            if (logicalData.getPdriGroupId() != 0) {
                s.executeUpdate(
                        "UPDATE pdrigroup_table "
                        + "SET refCount = refCount - 1 "
                        + "WHERE pdrigroup_table.groupId = " + logicalData.getPdriGroupId());
            }
            s.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = s.getGeneratedKeys();
            if (!rs.next()) {
                throw new CatalogueException("cannot get generated key after insert");
            }
            Long newGroupId = rs.getLong(1);
            s.executeUpdate("INSERT INTO pdri_table (url, storageSiteId, pdriGroupId) VALUES("
                    + "'" + pdri.getURL() + "', " + pdri.getStorageSiteId() + ", " + newGroupId + ")");

            s.executeUpdate("UPDATE ldata_table SET pdriGroupId = " + newGroupId + " WHERE uid = " + logicalData.getUID());
            logicalData.setPdriGroupId(newGroupId);
            return logicalData;
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Collection<PDRI> getPdriByGroupId(Long GroupId, Connection connection) throws CatalogueException {
        ArrayList<PDRI> res = new ArrayList<PDRI>();
        boolean connectionIsProvided;
        boolean connectionAutocommit = false;
        Statement s = null;
        if (connection == null) {
            connectionIsProvided = false;
            connection = getConnection();
        } else {
            connectionIsProvided = true;
        }
        try {
            connectionAutocommit = connection.getAutoCommit();
            s = connection.createStatement();
            ResultSet rs = s.executeQuery(
                    "SELECT url, username, password, storageSiteId FROM pdri_table "
                    + "JOIN storage_site_table ON pdri_table.storageSiteId = storage_site_table.storageSiteId "
                    + "JOIN credential_table ON storage_site_table.credentialRef = credential_table.credintialId "
                    + "WHERE pdri_table.pdriGroupId = " + GroupId);
            while (rs.next()) {
                res.add(PDRIFactory.getFactory().createInstance(
                        rs.getLong(4), rs.getString(1), rs.getString(2), rs.getString(3)));
            }
            return res;
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ILogicalData registerOrUpdateResourceEntry(ILogicalData entry, Connection connection) throws CatalogueException {
        boolean connectionIsProvided;
        boolean connectionAutocommit = false;
        PreparedStatement ps01 = null;
        if (connection == null) {
            connectionIsProvided = false;
            connection = getConnection();
        } else {
            connectionIsProvided = true;
        }
        try {
            connectionAutocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            ps01 = connection.prepareStatement(
                    "SELECT uid, ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table where ldata_table.parent = ? AND ldata_table.ld_name = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps01.setString(1, entry.getParent());
            ps01.setString(2, entry.getName());
            ResultSet rs = ps01.executeQuery();
            if (rs.next()) {
                // entry is registered already
                rs.updateString(2, entry.getOwner());
                rs.updateString(3, entry.getType());
                rs.updateTimestamp(7, new Timestamp(entry.getModifiedDate()));
                rs.updateLong(8, entry.getLength());
                rs.updateString(9, entry.getContentTypesAsString());
                rs.updateLong(10, entry.getPdriGroupId());
                rs.updateRow();
                entry.setUID(rs.getLong(1));
                entry.setCreateDate(rs.getTimestamp(6).getTime());
                return entry;
            } else {
                // no data
                rs.moveToInsertRow();
                rs.updateString(2, entry.getOwner());
                rs.updateString(3, entry.getType());
                rs.updateString(4, entry.getName());
                rs.updateString(5, entry.getParent());
                rs.updateTimestamp(6, new Timestamp(entry.getCreateDate()));
                rs.updateTimestamp(7, new Timestamp(entry.getModifiedDate()));
                rs.updateLong(8, entry.getLength());
                rs.updateString(9, entry.getContentTypesAsString());
                rs.updateLong(10, entry.getPdriGroupId());
                rs.insertRow();
                entry.setUID(rs.getLong(1));
                return entry;
            }
        } catch (Exception e) {
            try {
                if (ps01 != null && !ps01.isClosed()) {
                    ps01.close();
                    ps01 = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (ps01 != null && !ps01.isClosed()) {
                    ps01.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ILogicalData getResourceEntryByLDRI(Path logicalResourceName, Connection connection) throws Exception {
        ILogicalData res = null;
        boolean connectionIsProvided;
        boolean connectionAutocommit = false;
        PreparedStatement ps = null;
        if (connection == null) {
            connectionIsProvided = false;
            connection = getConnection();
        } else {
            connectionIsProvided = true;
        }
        try {
            connectionAutocommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(
                    "SELECT uid, ownerId, datatype, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table where ldata_table.parent = ? AND ldata_table.ld_name = ?");
            if (logicalResourceName.isRoot()) {
                ps.setString(1, "");
                ps.setString(2, "");
            } else {
                ps.setString(1, logicalResourceName.getParent().toPath());
                ps.setString(2, logicalResourceName.getName());
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Long UID = rs.getLong(1);
                String owner = rs.getString(2);
                String datatype = rs.getString(3);
                res = new LogicalData(logicalResourceName, datatype, this);
                res.setUID(UID);
                res.setOwner(owner);
                res.setCreateDate(rs.getTimestamp(4).getTime());
                res.setModifiedDate(rs.getTimestamp(5).getTime());
                res.setLength(rs.getLong(6));
                res.setContentTypesAsString(rs.getString(7));
                res.setPdriGroupId(rs.getLong(8));
            }
            return res;
        } catch (Exception e) {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                    ps = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setPermissions(Long UID, Permissions perm, Connection connection) throws CatalogueException {
        Statement s = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            s.addBatch("DELETE FROM permission_table WHERE permission_table.ld_uid_ref = " + UID);
            for (String cr : perm.canRead()) {
                s.addBatch("INSERT INTO permission_table (perm_type, ld_uid_ref, role_name) VALUES ('read', " + UID + " , '" + cr + "')");
            }
            for (String cw : perm.canWrite()) {
                s.addBatch("INSERT INTO permission_table (perm_type, ld_uid_ref, role_name) VALUES ('write', " + UID + " , '" + cw + "')");
            }
            s.executeBatch();
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Permissions getPermissions(Long UID, String owner, Connection connection) throws CatalogueException {
        Permissions p = new Permissions();
        p.setOwner(owner);
        Statement s = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT perm_type, role_name FROM permission_table WHERE permission_table.ld_uid_ref = " + UID);
            while (rs.next()) {
                if (rs.getString(1).equals("read")) {
                    p.canRead().add(rs.getString(2));
                } else {
                    p.canWrite().add(rs.getString(2));
                }
            }
            return p;
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ILogicalData getResourceEntryByUID(Long UID, Connection connection) throws CatalogueException {
        ILogicalData res = null;
        Statement s = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table WHERE ldata_table.uid = " + UID);
            if (rs.next()) {
                res = new LogicalData(this);
                res.setUID(UID);
                res.setOwner(rs.getString(1));
                res.setType(rs.getString(2));
                res.setName(rs.getString(3));
                res.setParent(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
            }
            return res;
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Collection<ILogicalData> getChildren(String parentPath, Connection connection) throws CatalogueException {
        LinkedList<ILogicalData> res = new LinkedList<ILogicalData>();
        PreparedStatement ps = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            ps = connection.prepareStatement("SELECT uid, ownerId, datatype, ld_name, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId  FROM ldata_table WHERE ldata_table.parent = ?");
            ps.setString(1, parentPath);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ILogicalData element = new LogicalData(this);
                element.setUID(rs.getLong(1));
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setParent(parentPath);
                element.setCreateDate(rs.getTimestamp(5).getTime());
                element.setModifiedDate(rs.getTimestamp(6).getTime());
                element.setLength(rs.getLong(7));
                element.setContentTypesAsString(rs.getString(8));
                element.setPdriGroupId(rs.getLong(9));
                res.add(element);
            }
            return res;
        } catch (Exception e) {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                    ps = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void moveEntry(ILogicalData toMove, ILogicalData newParent, String newName, Connection connection) throws CatalogueException {
        Statement s = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            ILogicalData entry = toMove;
            final String parentCurrentPathStr = entry.getLDRI().toPath();
            entry.setLDRI(Path.path(newParent.getLDRI(), newName));
            final String parentNewPathStr = entry.getLDRI().toPath();
            s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            s.executeUpdate("UPDATE ldata_table "
                    + "SET parent='" + newParent.getLDRI().toPath() + "', ld_name='" + newName + "' "
                    + "WHERE uid=" + entry.getUID());
            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                s.executeUpdate("UPDATE ldata_table "
                        + "SET parent=concat('" + parentNewPathStr + "', SUBSTRING(parent,"
                        + Integer.valueOf(parentCurrentPathStr.length() + 1).toString() + ")) "
                        + "WHERE parent LIKE '" + parentCurrentPathStr + "%'");
            }
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void copyEntry(Long entryId, List<Integer> perm, WebDataDirResource newParent, String newName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<MyStorageSite> getStorageSitesByUser(MyPrincipal user) throws CatalogueException {
        return null;
    }

    public Runnable deleteSweep() {
        final JDBCatalogue self = this;
        return new Runnable() {

            @Override
            public void run() {
                Connection connection = null;
                Statement s1 = null;
                Statement s2 = null;
                try {
                    connection = self.getConnection();
                    connection.setAutoCommit(false);
                    s1 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                            java.sql.ResultSet.CONCUR_UPDATABLE);
                    s2 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                            java.sql.ResultSet.CONCUR_UPDATABLE);
                    ResultSet rs1 = s1.executeQuery("SELECT groupId FROM pdrigroup_table WHERE refCount = 0");
                    while (rs1.next()) {
                        Long groupId = rs1.getLong(1);
                        ResultSet rs2 = s2.executeQuery(
                                "SELECT url, username, password, storageSiteId FROM pdri_table "
                                + "JOIN storage_site_table ON pdri_table.storageSiteId = storage_site_table.storageSiteId "
                                + "JOIN credential_table ON storage_site_table.credentialRef = credential_table.credintialId "
                                + "WHERE pdri_table.pdriGroupId = " + groupId);
                        while (rs2.next()) {
                            PDRI pdri = PDRIFactory.getFactory().createInstance(
                                    rs2.getLong(4), rs2.getString(1), rs2.getString(2), rs2.getString(3));
                            pdri.delete();
                        }
                        s2.executeUpdate("DELETE FROM pdri_table WHERE pdri_table.pdriGroupId = " + groupId);
                        rs1.deleteRow();
                    }
                } catch (Exception e) {
                    try {
                        if (s1 != null && !s1.isClosed()) {
                            s1.close();
                            s1 = null;
                        }
                        if (s2 != null && !s2.isClosed()) {
                            s2.close();
                            s2 = null;
                        }
                        if (connection != null && !connection.isClosed()) {
                            connection.rollback();
                            connection.close();
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, e);
                } finally {
                    try {
                        if (s1 != null && !s1.isClosed()) {
                            s1.close();
                        }
                        if (s2 != null && !s2.isClosed()) {
                            s2.close();
                        }
                        if (connection != null && !connection.isClosed()) {
                            connection.commit();
                            connection.close();
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }


            }
        };
    }

    public void putRolesToTmpTable(MyPrincipal principal, Connection connection) throws SQLException {
        Statement s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_UPDATABLE);
        s.executeUpdate("DROP TABLE IF EXISTS myroles");
        s.executeUpdate("CREATE TEMPORARY TABLE myroles (mrole VARCHAR(255))");
        for (String role : principal.getRoles()) {
            s.executeUpdate("INSERT INTO myroles(mrole) VALUES  ('" + role + "')");
        }
        s.close();
    }

    public void removeResourceEntry(ILogicalData toRemove, MyPrincipal principal, Connection connection) throws Exception {
        Statement s = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
            Permissions toRemovePerm = getPermissions(toRemove.getUID(), toRemove.getOwner(), connection);
            String parentPath = toRemove.getLDRI().toPath();
            if (toRemove.isFolder() && principal.canRead(toRemovePerm)
                    && principal.canWrite(toRemovePerm)) {
                s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_UPDATABLE);

                s.executeUpdate("DELETE FROM ldata_table "
                        + "WHERE ldata_table.parent = '" + parentPath + "' "
                        + "AND ldata_table.datatype = 'logical.file'");
                ResultSet rs = s.executeQuery("SELECT uid, ownerId, ld_name, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table "
                        + "WHERE ldata_table.parent = '" + parentPath + "' "
                        + "AND ldata_table.datatype = 'logical.folder'");
                LinkedList<ILogicalData> ld_list = new LinkedList<ILogicalData>();
                while (rs.next()) {
                    ILogicalData element = new LogicalData(this);
                    element.setUID(rs.getLong(1));
                    element.setOwner(rs.getString(2));
                    element.setType("logical.folder");
                    element.setName(rs.getString(3));
                    element.setParent(parentPath);
                    element.setCreateDate(rs.getTimestamp(4).getTime());
                    element.setModifiedDate(rs.getTimestamp(5).getTime());
                    element.setLength(rs.getLong(6));
                    element.setContentTypesAsString(rs.getString(7));
                    element.setPdriGroupId(rs.getLong(8));
                    ld_list.add(element);
                }
                rs.close();
                s.close();
                for (ILogicalData ld : ld_list) {
                    removeResourceEntry(ld, principal, connection);
                }
            }
            s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            s.executeUpdate("DELETE FROM ldata_table WHERE ldata_table.uid = " + toRemove.getUID());
        } catch (Exception e) {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                    s = null;
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new CatalogueException(e.getMessage());
        } finally {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
                if (!connectionIsProvided && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
                if (connectionIsProvided && !connection.isClosed()) {
                    connection.setAutoCommit(connectionAutocommit);
                }
            } catch (SQLException ex) {
                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

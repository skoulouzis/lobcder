/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;

/**
 *
 * @author dvasunin
 */
public class JDBCatalogue {

    private DataSource datasource = null;
    private static final boolean debug = true;

    public JDBCatalogue() throws NamingException, Exception {

        Runnable init = initDatasource();
        init.run();
    }

    public Connection getConnection() throws CatalogueException {
        try {
            if (datasource == null) {
                String jndiName = "jdbc/lobcder";
                Context ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                Context envContext = (Context) ctx.lookup("java:/comp/env");
                datasource = (DataSource) envContext.lookup(jndiName);
            }
            Connection cn = datasource.getConnection();
            cn.setAutoCommit(false);
            return cn;
        } catch (Exception e) {
            throw new CatalogueException(e.getMessage());
        }
    }
    private Timer timer = null;

    public void startSweep() {
        TimerTask gcTask = new TimerTask() {
            Runnable sweep = deleteSweep();

            @Override
            public void run() {
                sweep.run();
            }
        };
        timer = new Timer(true);
        timer.schedule(gcTask, 10000, 10000); //once in 10 sec
    }

    public void stopSweep() {
        timer.cancel();
    }

    public LogicalData registerPdriForNewEntry(LogicalData logicalData, PDRI pdri, Connection connection) throws CatalogueException {
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
            String sql = "INSERT INTO pdri_table (url, storageSiteId, pdriGroupId) VALUES("
                    + "'" + pdri.getURI() + "', " + pdri.getStorageSiteId() + ", " + newGroupId + ")";
            debug("##########################" + sql);

            s.executeUpdate(sql);


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
            connection.setAutoCommit(false);
            s = connection.createStatement();
            String sql = "SELECT url, pdri_table.storageSiteId AS storageSiteId, resourceURI, username, password FROM pdri_table "
                    + "JOIN storage_site_table ON pdri_table.storageSiteId = storage_site_table.storageSiteId "
                    + "JOIN credential_table ON storage_site_table.credentialRef = credential_table.credintialId "
                    + "WHERE pdri_table.pdriGroupId = " + GroupId;
            debug(sql);
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                String url = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                res.add(PDRIFactory.getFactory().createInstance(url, ssID, resourceURI, uName, passwd));
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

    public LogicalData registerOrUpdateResourceEntry(LogicalData entry, Connection connection) throws CatalogueException {
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
                    "SELECT uid, ownerId, datatype, ld_name, parent, createDate, "
                    + "modifiedDate, ld_length, contentTypesStr, pdriGroupId "
                    + "FROM ldata_table where ldata_table.parent = ? "
                    + "AND ldata_table.ld_name = ?", 
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            
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
                //
                // the driver adds rows at the end
                //
                rs.last();
                //
                // We should now be on the row we just inserted
                //              
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

    public LogicalData getResourceEntryByLDRI(Path logicalResourceName, Connection connection) throws Exception {
        LogicalData res = null;
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
                    "SELECT uid, ownerId, datatype, createDate, modifiedDate, ld_length, "
                    + "contentTypesStr, pdriGroupId, isSupervised, checksum, lastValidationDate, "
                    + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                    + "description, locationPreference "
                    + "FROM ldata_table where ldata_table.parent = ? AND ldata_table.ld_name = ?");
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
                res.setSupervised(rs.getBoolean(9));
                res.setChecksum(rs.getLong(10));
                res.setLastValidationDate(rs.getLong(11));
                res.setLockTokenID(rs.getString(12));
                res.setLockScope(rs.getString(13));
                res.setLockType(rs.getString(14));
                res.setLockedByUser(rs.getString(15));
                res.setLockDepth(rs.getString(16));
                res.setLockTimeout(rs.getLong(17));
                res.setDescription(rs.getString(18));
                res.setDataLocationPreference(rs.getString(19));
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
            for (String cr : perm.getRead()) {
                s.addBatch("INSERT INTO permission_table (perm_type, ld_uid_ref, role_name) VALUES ('read', " + UID + " , '" + cr + "')");
            }
            for (String cw : perm.getWrite()) {
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

    public void setPermissions(String parentDir, Permissions perm, MyPrincipal principal, Connection connection) throws CatalogueException {
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
            CallableStatement cs = connection.prepareCall("{call updatePermissionsProc(?, ?, ?, ?, ?, ?)}");
            cs.setString(1, principal.getUserId());
            cs.setString(2, principal.getRolesStr());
            cs.setString(3, perm.getOwner());
            cs.setString(4, perm.getReadStr());
            cs.setString(5, perm.getWriteStr());
            cs.setString(6, parentDir);
            cs.execute();
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
            Set<String> canRead = new HashSet<String>();
            Set<String> canWrite = new HashSet<String>();
            while (rs.next()) {
                if (rs.getString(1).equals("read")) {
                    canRead.add(rs.getString(2));
                } else {
                    canWrite.add(rs.getString(2));
                }
            }
            p.setRead(canRead);
            p.setWrite(canWrite);
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

    public LogicalData getResourceEntryByUID(Long UID, Connection connection) throws CatalogueException {
        LogicalData res = null;
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
            ResultSet rs = s.executeQuery("SELECT ownerId, datatype, ld_name, parent, "
                    + "createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId, "
                    + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                    + "lockType, lockedByUser, lockDepth, lockTimeout, description "
                    + "FROM ldata_table WHERE ldata_table.uid = " + UID);
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
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getLong(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(12));
                res.setLockScope(rs.getString(13));
                res.setLockType(rs.getString(14));
                res.setLockedByUser(rs.getString(15));
                res.setLockDepth(rs.getString(16));
                res.setLockTimeout(rs.getLong(17));
                res.setDescription(rs.getString(18));
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

    public Collection<LogicalData> getChildren(String parentPath, Connection connection) throws CatalogueException {
        LinkedList<LogicalData> res = new LinkedList<LogicalData>();
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
            ps = connection.prepareStatement("SELECT uid, ownerId, datatype, ld_name, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId, isSupervised  FROM ldata_table WHERE ldata_table.parent = ?");
            ps.setString(1, parentPath);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LogicalData element = new LogicalData(this);
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
                element.setSupervised(rs.getBoolean(10));
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

    public void moveEntry(LogicalData toMove, LogicalData newParent, String newName, Connection connection) throws CatalogueException {
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
            LogicalData entry = toMove;
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

    public void copyEntry(LogicalData toCopy, LogicalData newParent, String newName, MyPrincipal principal, Connection connection) throws CatalogueException {
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
            Permissions toCopyPerm = getPermissions(toCopy.getUID(), toCopy.getOwner(), connection);
            Permissions newParentPerm = getPermissions(newParent.getUID(), newParent.getOwner(), connection);
            Permissions permissionsForNew = new Permissions(principal);
            String parentPath = toCopy.getLDRI().toPath();
            if (toCopy.isFolder() && principal.canWrite(newParentPerm)) {
                LogicalData newFolderEntry = new LogicalData(Path.path(newParent.getLDRI(), newName), Constants.LOGICAL_FOLDER, this);
                newFolderEntry.setCreateDate(System.currentTimeMillis());
                newFolderEntry.setModifiedDate(System.currentTimeMillis());
                newFolderEntry.setOwner(principal.getUserId());
                newFolderEntry = registerOrUpdateResourceEntry(newFolderEntry, connection);
                setPermissions(newFolderEntry.getUID(), permissionsForNew, connection);
                if (principal.canRead(toCopyPerm)) {
                    CallableStatement cs = connection.prepareCall("{call copyFolderContentProc(?, ?, ?, ?, ?, ?)}");
                    cs.setString(1, principal.getUserId());
                    cs.setString(2, principal.getRolesStr());
                    cs.setString(3, permissionsForNew.getReadStr());
                    cs.setString(4, permissionsForNew.getWriteStr());
                    cs.setString(5, parentPath);
                    cs.setString(6, newFolderEntry.getLDRI().toPath());
                    cs.execute();
                    s = connection.createStatement();
                    ResultSet rs = s.executeQuery("SELECT uid, ownerId, ld_name, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table "
                            + "WHERE ldata_table.parent = '" + parentPath + "' "
                            + "AND ldata_table.datatype = 'logical.folder'");
                    LinkedList<LogicalData> ld_list = new LinkedList<LogicalData>();
                    while (rs.next()) {
                        LogicalData element = new LogicalData(this);
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
                    for (LogicalData ld : ld_list) {
                        copyEntry(ld, newFolderEntry, ld.getName(), principal, connection);
                    }
                }
            } else if (!toCopy.isFolder() && principal.canRead(toCopyPerm) && principal.canWrite(newParentPerm)) {
                LogicalData ld = (LogicalData) toCopy.clone();
                ld.setOwner(principal.getUserId());
                ld.setName(newName);
                ld.setParent(newParent.getLDRI().toPath());
                ld.setCreateDate(System.currentTimeMillis());
                ld.setModifiedDate(System.currentTimeMillis());
                ld = registerOrUpdateResourceEntry(ld, connection);
                setPermissions(ld.getUID(), permissionsForNew, connection);
                s = connection.createStatement();
                s.executeUpdate("UPDATE pdrigroup_table SET refCount=refCount+1 WHERE groupId = " + ld.getPdriGroupId());
                s.close();
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

    public Collection<MyStorageSite> getStorageSitesByUser(MyPrincipal user, Connection connection) throws CatalogueException {
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
            boolean first = true;
            String sql = "SELECT DISTINCT storageSiteId, resourceURI, currentNum, currentSize, quotaNum, quotaSize, username, password FROM role_to_ss_table JOIN storage_site_table ON ss_id = storageSiteId JOIN credential_table ON credentialRef = credintialId WHERE ";
            for (String role : user.getRoles()) {
                if (first) {
                    sql += "role_name = '" + role + "'";
                    first = false;
                } else {
                    sql += " OR role_name = '" + role + "'";
                }
            }
            s = connection.createStatement();
            debug(sql);
            ResultSet rs = s.executeQuery(sql);
            LinkedList<MyStorageSite> res = new LinkedList<MyStorageSite>();
            while (rs.next()) {
                Credential c = new Credential();
                c.setStorageSiteUsername(rs.getString(7));
                c.setStorageSitePassword(rs.getString(8));
                MyStorageSite ss = new MyStorageSite();
                ss.setStorageSiteId(rs.getLong(1));
                ss.setCredential(c);
                ss.setResourceURI(rs.getString(2));
                ss.setCurrentNum(rs.getLong(3));
                ss.setCurrentSize(rs.getLong(4));
                ss.setQuotaNum(rs.getLong(5));
                ss.setQuotaSize(rs.getLong(6));
                res.add(ss);
            }
            rs.close();
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
                                "SELECT url, pdri_table.storageSiteId AS storageSiteId, resourceURI, username, password FROM pdri_table "
                                + "JOIN storage_site_table ON pdri_table.storageSiteId = storage_site_table.storageSiteId "
                                + "JOIN credential_table ON storage_site_table.credentialRef = credential_table.credintialId "
                                + "WHERE pdri_table.pdriGroupId = " + groupId);
                        while (rs2.next()) {
                            String url = rs2.getString(1);
                            long ssID = rs2.getLong(2);
                            String resourceURI = rs2.getString(3);
                            String uName = rs2.getString(4);
                            String passwd = rs2.getString(5);
                            PDRI pdri = PDRIFactory.getFactory().createInstance(url, ssID, resourceURI, uName, passwd);
                            pdri.delete();
                        }
                        s2.executeUpdate("DELETE FROM pdri_table WHERE pdri_table.pdriGroupId = " + groupId);
                        rs1.deleteRow();
                        rs1.beforeFirst();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

    public void removeResourceEntry(LogicalData toRemove, MyPrincipal principal, Connection connection) throws CatalogueException {
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
                LinkedList<LogicalData> ld_list = new LinkedList<LogicalData>();
                while (rs.next()) {
                    LogicalData element = new LogicalData(this);
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
                for (LogicalData ld : ld_list) {
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
                throw new CatalogueException(ex.getMessage());
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
                throw new CatalogueException(ex.getMessage());
            }
        }
    }

    public List<LogicalData> getSupervised(String parentDir, Connection connection) throws CatalogueException {
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
            ResultSet rs = s.executeQuery("SELECT uid, ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId, checksum, lastValidationDate FROM ldata_table WHERE ldata_table.isSupervised = TRUE AND datatype = 'logical.file' AND parent LIKE '" + parentDir + "%'");
            LinkedList<LogicalData> ld_list = new LinkedList<LogicalData>();
            while (rs.next()) {
                LogicalData element = new LogicalData(this);
                element.setUID(rs.getLong(1));
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setParent(rs.getString(5));
                element.setCreateDate(rs.getTimestamp(6).getTime());
                element.setModifiedDate(rs.getTimestamp(7).getTime());
                element.setLength(rs.getLong(8));
                element.setContentTypesAsString(rs.getString(9));
                element.setPdriGroupId(rs.getLong(10));
                element.setChecksum(rs.getLong(11));
                element.setLastValidationDate(rs.getLong(12));
                ld_list.add(element);
            }
            rs.close();
            s.close();
            return ld_list;
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

    public void setDirSupervised2(String parentDir, Boolean flag, MyPrincipal principal, Connection connection) throws CatalogueException {
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
            String parent = Path.path(parentDir).getParent().toPath();
            String name = Path.path(parentDir).getName();

            CallableStatement cs = connection.prepareCall("{call updateDriFlagProc(?, ?, ?, ?)}");
            cs.setString(1, principal.getUserId());
            cs.setString(2, principal.getRolesStr());
            cs.setBoolean(3, flag);
            cs.setString(4, parentDir);
            cs.execute();
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

    public void setDirSupervised(String parentDir, Boolean flag, Connection connection) throws CatalogueException {
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
            String parent = Path.path(parentDir).getParent().toPath();
            String name = Path.path(parentDir).getName();
            s = connection.createStatement();
            s.executeUpdate("UPDATE ldata_table SET isSupervised = " + flag.toString() + " WHERE parent = '" + parent + "' AND ld_name = '" + name + "'");
            s.executeUpdate("UPDATE ldata_table SET isSupervised = " + flag.toString() + " WHERE parent LIKE '" + parentDir + "%'");
            s.close();
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

    public void setFileSupervised(Long uid, Boolean flag, Connection connection) throws CatalogueException {
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
            s.executeUpdate("UPDATE ldata_table SET isSupervised = " + flag.toString() + " WHERE uid = " + uid);
            s.close();
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

    public void setLastValidationDate(Long uid, Long lastValidationDate, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lastValidationDate = ? WHERE uid = ?");
            ps.setLong(1, lastValidationDate);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setFileChecksum(Long uid, Long checksum, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET checksum = ? WHERE uid = ?");
            ps.setLong(1, checksum);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockTokenID(Long uid, String lockTokenID, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockTokenID = ? WHERE uid = ?");
            ps.setString(1, lockTokenID);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockTimeout(Long uid, Long lockTimeout, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockTimeout = ? WHERE uid = ?");
            ps.setLong(1, lockTimeout);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockDepth(Long uid, String lockDepth, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockDepth = ? WHERE uid = ?");
            ps.setString(1, lockDepth);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockByUser(Long uid, String lockedByUser, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockedByUser = ? WHERE uid = ?");
            ps.setString(1, lockedByUser);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockScope(Long uid, String lockScope, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockScope = ? WHERE uid = ?");
            ps.setString(1, lockScope);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void setLockType(Long uid, String lockType, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET lockType = ? WHERE uid = ?");
            ps.setString(1, lockType);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getName() + ": " + msg);
        }
    }

    private Runnable initDatasource() {
        return new Runnable() {
            @Override
            public void run() {
                if (datasource == null) {
                    try {
                        String jndiName = "jdbc/lobcder";
                        Context ctx = new InitialContext();
                        if (ctx == null) {
                            throw new Exception("JNDI could not create InitalContext ");
                        }
                        Context envContext = (Context) ctx.lookup("java:/comp/env");
                        datasource = (DataSource) envContext.lookup(jndiName);
                    } catch (Exception ex) {
                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
    }

    public LinkedList<LogicalData> queryLogicalData(MultivaluedMap<String, String> queryParameters, Connection connection) throws CatalogueException {
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
            boolean where = false;
            StringBuilder query = new StringBuilder("SELECT uid, ownerId, datatype, "
                    + "ld_name, parent, createDate, modifiedDate, ld_length, "
                    + "contentTypesStr, pdriGroupId, isSupervised, checksum, "
                    + "lastValidationDate, lockTokenID, lockScope, "
                    + "lockType, lockedByUser, lockDepth, lockTimeout, description "
                    + "FROM ldata_table");
            if (queryParameters.containsKey("path") && queryParameters.get("path").iterator().hasNext()) {
                String path = queryParameters.get("path").iterator().next();
                if (!path.equals("/")) {
                    query.append(where ? " AND" : " WHERE").append(" parent LIKE '").append(path).append("%'");
                    where = true;
                }
                queryParameters.remove("path");
            }
            if (queryParameters.containsKey("mStartDate") && queryParameters.get("mStartDate").iterator().hasNext()
                    && queryParameters.containsKey("mEndDate") && queryParameters.get("mEndDate").iterator().hasNext()) {
                String mStartDate = Long.valueOf(queryParameters.get("mStartDate").iterator().next()).toString();
                String mEndDate = Long.valueOf(queryParameters.get("mEndDate").iterator().next()).toString();
                        query.append(where ? " AND" : " WHERE").append(" modifiedDate BETWEEN FROM_UNIXTIME(").append(mStartDate).append(") AND FROM_UNIXTIME(").append(mEndDate).append(")");
                where = true;
                queryParameters.remove("mStartDate");
                queryParameters.remove("mEndDate");
            } else if (queryParameters.containsKey("mStartDate") && queryParameters.get("mStartDate").iterator().hasNext()) {
                String mStartDate = Long.valueOf(queryParameters.get("mStartDate").iterator().next()).toString();
                query.append(where ? " AND" : " WHERE").append(" modifiedDate >= UNIXTIME(").append(mStartDate).append(")");
                where = true;
                queryParameters.remove("mStartDate");
            } else if (queryParameters.containsKey("mEndDate") && queryParameters.get("mEndDate").iterator().hasNext()) {
                String mEndDate = Long.valueOf(queryParameters.get("mEndDate").iterator().next()).toString();
                query.append(where ? " AND" : " WHERE").append(" modifiedDate <= UNIXTIME(").append(mEndDate).append(")");
                where = true;
                queryParameters.remove("mEndDate");
            }
            if (queryParameters.containsKey("isSupervised") && queryParameters.get("isSupervised").iterator().hasNext()) {
                String isSupervised = Boolean.valueOf(queryParameters.get("isSupervised").iterator().next()).toString();
                query.append(where ? " AND" : " WHERE").append(" isSupervised = ").append(isSupervised);
                where = true;
                queryParameters.remove("isSupervised");
            }
            debug("queryLogicalData() SQL: " + query.toString());
            ResultSet rs = s.executeQuery(query.toString());
            LinkedList<LogicalData> ld_list = new LinkedList<LogicalData>();
            while (rs.next()) {
                LogicalData element = new LogicalData(this);
                element.setUID(rs.getLong(1));
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setParent(rs.getString(5));
                element.setCreateDate(rs.getTimestamp(6).getTime());
                element.setModifiedDate(rs.getTimestamp(7).getTime());
                element.setLength(rs.getLong(8));
                element.setContentTypesAsString(rs.getString(9));
                element.setPdriGroupId(rs.getLong(10));
                element.setSupervised(rs.getBoolean(11));
                element.setChecksum(rs.getLong(12));
                element.setLastValidationDate(rs.getLong(13));
//                element.setLockTokenID(rs.getString(14));
//                element.setLockScope(rs.getString(15));
//                element.setLockType(rs.getString(16));
//                element.setLockedByUser(rs.getString(17));
//                element.setLockDepth(rs.getString(18));
//                element.setLockTimeout(rs.getLong(19));
                element.setDescription(rs.getString(14));
                ld_list.add(element);
            }
            s.close();
            return ld_list;
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

    public void setDescription(Long uid, String description, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET description = ? WHERE uid = ?");
            ps.setString(1, description);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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

    public void registerStorageSite(String resourceURI, Credential credentials, int currentNum, int currentSize, int quotaNum, int quotaSize, Connection connection) throws CatalogueException {
        PreparedStatement ps = null;
        boolean connectionIsProvided = (connection == null) ? false : true;
        boolean connectionAutocommit = false;
        PreparedStatement ps01 = null;
        try {
            if (connection == null) {
                connection = getConnection();
                connection.setAutoCommit(false);
            } else {
                connectionAutocommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
            }
//            ps01 = connection.prepareStatement(
//                    "SELECT uid, ownerId, datatype, ld_name, parent, createDate, modifiedDate, ld_length, contentTypesStr, pdriGroupId FROM ldata_table where ldata_table.parent = ? AND ldata_table.ld_name = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
//            ps01.setString(1, entry.getParent());
//            ps01.setString(2, entry.getName());
//            ResultSet rs = ps01.executeQuery();


        } catch (SQLException ex) {
            throw new CatalogueException(ex.getMessage());
        }
    }

    public void setLocationPreference(Long uid, String locationPreference, Connection connection) throws CatalogueException {
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
            ps = connection.prepareStatement("UPDATE ldata_table SET locationPreference = ? WHERE uid = ?");
            ps.setString(1, locationPreference);
            ps.setLong(2, uid);
            ps.executeUpdate();
            ps.close();
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
                throw new CatalogueException(ex.getMessage());
            }
        }
    }
}

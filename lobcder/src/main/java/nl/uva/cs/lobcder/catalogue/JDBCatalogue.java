/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import io.milton.common.Path;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.MyDataSource;

import javax.annotation.Nonnull;
import javax.naming.NamingException;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 *
 * @author dvasunin
 */
@Log
public class JDBCatalogue extends MyDataSource {

    private Timer timer = null;

    public JDBCatalogue() throws NamingException {
    }

    public void startSweep() {
        TimerTask gcTask = new TimerTask() {

            Runnable deleteSweep = new DeleteSweep(getDatasource());
            Runnable replicateSweep = new ReplicateSweep(getDatasource());
            
            @Override
            public void run() {
                deleteSweep.run();
                replicateSweep.run();
            }
        };
        timer = new Timer(true);
        timer.schedule(gcTask, 10000, 10000); //once in 10 sec
    }

    public void stopSweep() {
        timer.cancel();
//        timer.purge();
    }

    public LogicalData registerDirLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate)"
                        + " VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, Constants.LOGICAL_FOLDER);
            preparedStatement.setString(4, entry.getName());
            preparedStatement.setDate(5, new Date(entry.getCreateDate()));
            preparedStatement.setDate(6, new Date(entry.getModifiedDate()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
            return entry;
        }
    }

    public LogicalData registerLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO ldata_table(parentRef, ownerId, datatype, createDate, modifiedDate,"
                        + "ldLength, contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate,"
                        + "lockTokenId, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, ldName) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, entry.getType());
            preparedStatement.setDate(4, new Date(entry.getCreateDate()));
            preparedStatement.setDate(5, new Date(entry.getModifiedDate()));
            preparedStatement.setLong(6, entry.getLength());
            preparedStatement.setString(7, entry.getContentTypesAsString());
            preparedStatement.setLong(8, entry.getPdriGroupId());
            preparedStatement.setBoolean(9, entry.getSupervised());
            preparedStatement.setLong(10, entry.getChecksum());
            preparedStatement.setLong(11, entry.getLastValidationDate());
            preparedStatement.setString(12, entry.getLockTokenID());
            preparedStatement.setString(13, entry.getLockScope());
            preparedStatement.setString(14, entry.getLockType());
            preparedStatement.setString(15, entry.getLockedByUser());
            preparedStatement.setString(16, entry.getLockDepth());
            preparedStatement.setLong(17, entry.getLockTimeout());
            preparedStatement.setString(18, entry.getDescription());
            preparedStatement.setString(19, entry.getDataLocationPreference());
            preparedStatement.setString(20, entry.getName());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
            return entry;
        }
    }

    public LogicalData updateLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
//        "UPDATE lobcder.ldata_table SET ldName = testFileName1.txtsdsdsdd, ldLength = 23231 WHERE uid = 44"
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET modifiedDate = ?, "
                        + "ldLength = ?, "
                        + "contentTypesStr = ?, "
                        + "pdriGroupRef = ? "
                        + "WHERE uid = ?")) {
            ps.setTimestamp(1, new Timestamp(entry.getModifiedDate()));
            ps.setLong(2, entry.getLength());
            ps.setString(3, entry.getContentTypesAsString());
            ps.setLong(4, entry.getPdriGroupId());
            ps.setLong(5, entry.getUid());
            ps.executeUpdate();
            return entry;
        }
    }

    public LogicalData updateLogicalDataAndPdri(LogicalData logicalData, PDRI pdri, @Nonnull Connection connection) throws SQLException {
//        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
//            statement.executeUpdate("DELETE FROM pdri_table WHERE pdri_table.pdriId = " + groupId);
//        }
        
        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
            statement.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            Long newGroupId = rs.getLong(1);
            
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table "
                    + "(fileName, storageSiteRef, pdriGroupRef) VALUES(?, ?, ?)")) {
                preparedStatement.setString(1, pdri.getFileName());
                preparedStatement.setLong(2, pdri.getStorageSiteId());
                preparedStatement.setLong(3, newGroupId);
                preparedStatement.executeUpdate();
                logicalData.setPdriGroupId(newGroupId);
                return updateLogicalData(logicalData, connection);
            }
        }
    }

    public LogicalData associateLogicalDataAndPdri(LogicalData logicalData, PDRI pdri, @Nonnull Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
            statement.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            Long newGroupId = rs.getLong(1);
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table (fileName, storageSiteRef, pdriGroupRef) VALUES(?, ?, ?)")) {
                preparedStatement.setString(1, pdri.getFileName());
                preparedStatement.setLong(2, pdri.getStorageSiteId());
                preparedStatement.setLong(3, newGroupId);
                preparedStatement.executeUpdate();
                logicalData.setPdriGroupId(newGroupId);
                return registerLogicalData(logicalData, connection);
            }
        }
    }

    public List<PDRIDescr> getPdriDescrByGroupId(Long groupId) throws SQLException, IOException {
        try (Connection connection = getConnection()) {
            return getPdriDescrByGroupId(groupId, connection);
        }
    }

    public List<PDRIDescr> getPdriDescrByGroupId(Long groupId, @Nonnull Connection connection) throws SQLException {
        ArrayList<PDRIDescr> res = new ArrayList<PDRIDescr>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT fileName, storageSiteRef, resourceURI, username, password FROM pdri_table "
                        + "JOIN storage_site_table ON storageSiteRef = storageSiteId "
                        + "JOIN credential_table ON credentialRef = credintialId "
                        + "WHERE pdri_table.pdriGroupRef = ?")) {
            ps.setLong(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String fileName = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                res.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd));
            }
            return res;
        }
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName) throws SQLException {
        try (Connection connection = getConnection()) {
            return getLogicalDataByPath(logicalResourceName, connection);
        }
    }

    public Long getLogicalDataUidByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 0;
            for (String p : logicalResourceName.getParts()) {
                preparedStatement.setLong(1, parent);
                preparedStatement.setString(2, p);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    parent = rs.getLong(1);
                } else {
                    return null;
                }
            }
            return parent;
        }
    }

    public Long getLogicalDataUidByParentRefAndName(Long parentRef, String name, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            preparedStatement.setLong(1, parentRef);
            preparedStatement.setString(2, name);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return null;
            }
        }
    }

    public LogicalData getLogicalDataByParentRefAndName(Long parentRef, String name, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                        + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                        + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                        + "description, locationPreference "
                        + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            preparedStatement.setLong(1, parentRef);
            preparedStatement.setString(2, name);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(rs.getLong(1));
                res.setParentRef(parentRef);
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(name);
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
                return res;
            } else {
                return null;
            }

        }

    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
                if (i == (parts.length - 1)) {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(
                                    "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                                    + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                                    + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                                    + "description, locationPreference "
                                    + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            LogicalData res = new LogicalData();
                            res.setUid(rs.getLong(1));
                            res.setParentRef(parent);
                            res.setOwner(rs.getString(2));
                            res.setType(rs.getString(3));
                            res.setName(p);
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
                            return res;
                        } else {
                            return null;
                        }
                    }
                } else {
                    preparedStatement.setLong(1, parent);
                    preparedStatement.setString(2, p);
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        parent = rs.getLong(1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    public LogicalData getLogicalDataByUid(Long UID) throws SQLException {
        try (Connection connection = getConnection()) {
            return getLogicalDataByUid(UID, connection);
        }
    }

    public LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                        + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                        + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                        + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference "
                        + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(UID);
                res.setParentRef(rs.getLong(1));
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getLong(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));
                res.setDataLocationPreference(rs.getString(20));
                return res;
            } else {
                return null;
            }
        }
    }

    public void setPermissions(Long UID, Permissions perm) throws SQLException {
        try (Connection connection = getConnection()) {
            try {
                setPermissions(UID, perm, connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void setPermissions(Long UID, Permissions perm, @Nonnull Connection connection) throws SQLException {
        try (Statement s = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_UPDATABLE)) {
            s.addBatch("DELETE FROM permission_table WHERE permission_table.ldUidRef = " + UID);
            for (String cr : perm.getRead()) {
                s.addBatch("INSERT INTO permission_table (permType, ldUidRef, roleName) VALUES ('read', " + UID + " , '" + cr + "')");
            }
            for (String cw : perm.getWrite()) {
                s.addBatch("INSERT INTO permission_table (permType, ldUidRef, roleName) VALUES ('write', " + UID + " , '" + cw + "')");
            }
            s.executeBatch();
        }
    }

    public Permissions getPermissions(Long UID, String owner) throws SQLException {
        try (Connection connection = getConnection()) {
            return getPermissions(UID, owner, connection);
        }
    }

    public Permissions getPermissions(Long UID, @Nonnull String owner, @Nonnull Connection connection) throws SQLException {
        Permissions p = new Permissions();
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT permType, roleName FROM permission_table WHERE permission_table.ldUidRef = " + UID);
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
            p.setOwner(owner);
            return p;
        }
    }

    public Collection<LogicalData> getChildrenByParentRef(Long parentRef) throws SQLException {
        try (Connection connection = getConnection()) {
            try {
                Collection<LogicalData> res = getChildrenByParentRef(parentRef, connection);
                connection.commit();
                return res;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public Collection<LogicalData> getChildrenByParentRef(Long parentRef, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                        + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                        + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                        + "description, locationPreference "
                        + "FROM ldata_table WHERE ldata_table.parentRef = ?")) {
            preparedStatement.setLong(1, parentRef);
            ResultSet rs = preparedStatement.executeQuery();
            LinkedList<LogicalData> res = new LinkedList<LogicalData>();
            while (rs.next()) {
                LogicalData element = new LogicalData();
                element.setUid(rs.getLong(1));
                element.setParentRef(parentRef);
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setCreateDate(rs.getTimestamp(5).getTime());
                element.setModifiedDate(rs.getTimestamp(6).getTime());
                element.setLength(rs.getLong(7));
                element.setContentTypesAsString(rs.getString(8));
                element.setPdriGroupId(rs.getLong(9));
                element.setSupervised(rs.getBoolean(10));
                element.setChecksum(rs.getLong(11));
                element.setLastValidationDate(rs.getLong(12));
                element.setLockTokenID(rs.getString(13));
                element.setLockScope(rs.getString(14));
                element.setLockType(rs.getString(15));
                element.setLockedByUser(rs.getString(16));
                element.setLockDepth(rs.getString(17));
                element.setLockTimeout(rs.getLong(18));
                element.setDescription(rs.getString(19));
                element.setDataLocationPreference(rs.getString(20));
                res.add(element);
            }
            return res;
        }
    }

    public void moveEntry(LogicalData toMove, LogicalData newParent, String newName) throws SQLException {
        try (Connection connection = getConnection()) {
            try {
                moveEntry(toMove, newParent, newName, connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void moveEntry(LogicalData toMove, LogicalData newParent, String newName, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET parentRef = ?, ldName = ? WHERE uid = ?")) {
            ps.setLong(1, newParent.getUid());
            ps.setString(2, newName);
            ps.setLong(3, toMove.getUid());
            ps.executeUpdate();
        }
    }

    @SneakyThrows(CloneNotSupportedException.class)
    public void copyFolder(LogicalData toCopy, LogicalData newParent, String newName, MyPrincipal principal, Connection connection) throws SQLException {
        Permissions toCopyPerm = getPermissions(toCopy.getUid(), toCopy.getOwner(), connection);
        Permissions newParentPerm = getPermissions(newParent.getUid(), newParent.getOwner(), connection);
        Permissions permissionsForNew = new Permissions(principal);
        if (toCopy.isFolder() && principal.canWrite(newParentPerm)) {
            // ignore folder if there is a problem
            LogicalData newFolderEntry = toCopy.clone();
            newFolderEntry.setName(newName);
            newFolderEntry.setParentRef(newParent.getUid());
            newFolderEntry.setType(Constants.LOGICAL_FOLDER);
            newFolderEntry.setCreateDate(System.currentTimeMillis());
            newFolderEntry.setModifiedDate(System.currentTimeMillis());
            newFolderEntry.setOwner(principal.getUserId());
            newFolderEntry = registerDirLogicalData(newFolderEntry, connection);
            setPermissions(newFolderEntry.getUid(), permissionsForNew, connection);
            if (principal.canRead(toCopyPerm)) {
                try (CallableStatement cs = connection.prepareCall("{CALL copyFolderContentProc(?, ?, ?, ?, ?, ?)}")) {
                    cs.setString(1, principal.getUserId());
                    cs.setString(2, principal.getRolesStr());
                    cs.setString(3, permissionsForNew.getReadStr());
                    cs.setString(4, permissionsForNew.getWriteStr());
                    cs.setLong(5, toCopy.getUid());
                    cs.setLong(6, newFolderEntry.getUid());
                    cs.execute();
                    try (PreparedStatement ps1 = connection.prepareStatement(
                                    "SELECT uid, ownerId, ldName FROM ldata_table WHERE datatype='logical.folder' AND parentRef = ?")) {
                        ps1.setLong(1, toCopy.getUid());
                        ResultSet rs = ps1.executeQuery();
                        while (rs.next()) {
                            LogicalData element = new LogicalData();
                            element.setUid(rs.getLong(1));
                            element.setOwner(rs.getString(2));
                            element.setType("logical.folder");
                            element.setName(rs.getString(3));
                            element.setParentRef(toCopy.getUid());
                            copyFolder(element, newFolderEntry, element.getName(), principal, connection);
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows(CloneNotSupportedException.class)
    public void copyFile(LogicalData toCopy, LogicalData newParent, String newName, MyPrincipal principal, Connection connection) throws SQLException {
        Permissions toCopyPerm = getPermissions(toCopy.getUid(), toCopy.getOwner(), connection);
        Permissions newParentPerm = getPermissions(newParent.getUid(), newParent.getOwner(), connection);
        Permissions permissionsForNew = new Permissions(principal);
        if (!toCopy.isFolder() && principal.canRead(toCopyPerm) && principal.canWrite(newParentPerm)) {
            LogicalData newFileEntry = toCopy.clone();
            newFileEntry.setUid(Long.valueOf(0));
            newFileEntry.setOwner(principal.getUserId());
            newFileEntry.setName(newName);
            newFileEntry.setParentRef(newParent.getUid());
            newFileEntry.setCreateDate(System.currentTimeMillis());
            newFileEntry.setModifiedDate(System.currentTimeMillis());
            newFileEntry = registerLogicalData(newFileEntry, connection);
            setPermissions(newFileEntry.getUid(), permissionsForNew, connection);
            try (Statement s = connection.createStatement()) {
                s.executeUpdate("UPDATE pdrigroup_table SET refCount=refCount+1 WHERE groupId = " + newFileEntry.getPdriGroupId());
            }
        }
    }

    public void remove(LogicalData toRemove, MyPrincipal principal, Connection connection) throws SQLException {
        boolean flag = true;
        if (toRemove.isFolder()) {
            flag = removeFolderContent(toRemove, principal, connection);
        }
        if (flag) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ldata_table WHERE uid = ?")) {
                ps.setLong(1, toRemove.getUid());
                ps.executeUpdate();
            }
        }
    }

    public boolean removeFolderContent(LogicalData toRemove, MyPrincipal principal, Connection connection) throws SQLException {
        boolean flag = true;
        Permissions toRemovePerm = getPermissions(toRemove.getUid(), toRemove.getOwner(), connection);
        if (principal.canRead(toRemovePerm)
                && principal.canWrite(toRemovePerm)) {
            try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM ldata_table WHERE datatype  = 'logical.file' AND parentRef = ?")) {
                ps1.setLong(1, toRemove.getUid());
                ps1.executeUpdate();
                try (PreparedStatement ps2 = connection.prepareStatement("SELECT uid, ownerId FROM ldata_table WHERE datatype  = 'logical.folder' AND parentRef = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
                    ps2.setLong(1, toRemove.getUid());
                    ResultSet rs = ps2.executeQuery();
                    while (rs.next()) {
                        LogicalData element = new LogicalData();
                        element.setUid(rs.getLong(1));
                        element.setParentRef(toRemove.getUid());
                        element.setType(Constants.LOGICAL_FOLDER);
                        element.setOwner(rs.getString(2));
                        if (removeFolderContent(element, principal, connection)) {
                            rs.deleteRow();
                        } else {
                            flag = false;
                        }
                    }
                }
            }
        } else {
            return false;
        }
        return flag;
    }

    public void setLogicalDataSupervised(@Nonnull Long uid, @Nonnull Boolean flag, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET isSupervised = ? WHERE uid = ?")) {
            ps.setBoolean(1, flag);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLastValidationDate(@Nonnull Long uid, @Nonnull Long lastValidationDate, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lastValidationDate = ? WHERE uid = ?")) {
            ps.setLong(1, lastValidationDate);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setFileChecksum(@Nonnull Long uid, @Nonnull Long checksum, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET checksum = ? WHERE uid = ?")) {
            ps.setLong(1, checksum);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockTokenID(@Nonnull Long uid, @Nonnull String lockTokenID, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockTokenID = ? WHERE uid = ?")) {
            ps.setString(1, lockTokenID);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockTimeout(Long uid, Long lockTimeout, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockTimeout = ? WHERE uid = ?")) {
            ps.setLong(1, lockTimeout);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockDepth(Long uid, String lockDepth, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockDepth = ? WHERE uid = ?")) {
            ps.setString(1, lockDepth);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockByUser(Long uid, String lockedByUser, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockedByUser = ? WHERE uid = ?")) {
            ps.setString(1, lockedByUser);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockScope(Long uid, String lockScope, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockScope = ? WHERE uid = ?")) {
            ps.setString(1, lockScope);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setLockType(Long uid, String lockType, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET lockType = ? WHERE uid = ?")) {
            ps.setString(1, lockType);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    /*
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
    JDBCatalogue.log.fine("queryLogicalData() SQL: " + query.toString());
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
     */
    public void setDescription(Long uid, String description, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET description = ? WHERE uid = ?")) {
            ps.setString(1, description);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void registerStorageSite(String resourceURI, Credential credentials, int currentNum, int currentSize, int quotaNum, int quotaSize, Connection connection) throws CatalogueException {
    }

    public void setLocationPreference(Long uid, String locationPreference, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET locationPreference = ? WHERE uid = ?")) {
            ps.setString(1, locationPreference);
            ps.setLong(2, uid);
            ps.executeUpdate();


        }
    }

    @Data
    @AllArgsConstructor
    public class PathInfo {

        private String name;
        private Long parentRef;
    }

    private void getPathforLogicalData(PathInfo pi, List<PathInfo> pil, PreparedStatement ps) throws SQLException {
        ps.setLong(1, pi.getParentRef());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                pi = new PathInfo(rs.getString(1), rs.getLong(2));
                pil.add(pi);
            }
        }
        if (pi != null && pi.getParentRef() != 1) {
            getPathforLogicalData(pi, pil, ps);
        }
    }

    public String getPathforLogicalData(LogicalData ld) throws SQLException {
        try (Connection connection = getConnection()) {
            return getPathforLogicalData(ld, connection);
        }
    }

    public String getPathforLogicalData(LogicalData ld, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT ldName, parentRef FROM ldata_table WHERE uid = ?")) {
            PathInfo pi = new PathInfo(ld.getName(), ld.getParentRef());
            List<PathInfo> pil = new ArrayList<>();
            getPathforLogicalData(pi, pil, ps);
            String res = "/";
            Collections.reverse(pil);
            for (PathInfo pi1 : pil) {
                res += pi1.getName();
            }
            return res;
        }
    }

    public void updateOwner(@Nonnull Long uid, @Nonnull String owner, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET ownerId = ? WHERE uid = ?")) {
            ps.setString(1, owner);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }
}

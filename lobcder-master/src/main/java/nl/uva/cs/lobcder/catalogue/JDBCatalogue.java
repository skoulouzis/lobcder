/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import io.milton.common.Path;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.frontend.RequestWapper;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.rest.wrappers.UsersWrapper;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.vrl.VRL;

/**
 *
 * @author dvasunin
 */
public class JDBCatalogue extends MyDataSource {

    private static boolean usePDRIDescrCache = false;
    private Timer timer;
    private static Map<Long, List<PDRIDescr>> PDRIDescrCache = new ConcurrentHashMap<>();
    private static Map<Long, LogicalData> LogicalDataCache = new ConcurrentHashMap<>();
    private static int cacheSize = 1000;

    public JDBCatalogue() throws NamingException {
    }

    public void startSweep() throws Exception {
        TimerTask gcTask = new SweepersTimerTask(getDatasource());
        timer = new Timer(true);
        timer.schedule(gcTask, PropertiesHelper.getSweepersInterval(), PropertiesHelper.getSweepersInterval());
//        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//        timer = scheduler.scheduleWithFixedDelay(gcTask, PropertiesHelper.getSweepersInterval(), PropertiesHelper.getSweepersInterval(), TimeUnit.MILLISECONDS);
//        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
//        timer = exec.scheduleWithFixedDelay(gcTask, PropertiesHelper.getSweepersInterval(), PropertiesHelper.getSweepersInterval(), TimeUnit.MILLISECONDS);
    }

    public void stopSweep() throws ExecutionException, TimeoutException, InterruptedException {
        if (timer != null) {
//            if (!timer.isDone()) {
//                Thread.sleep(500);
//            }
//            timer.get(500, TimeUnit.MICROSECONDS);
//            timer.cancel(true);
            timer.purge();
            timer.cancel();
            timer = null;
        }
    }

    public Collection<StorageSite> getStorageSites(Boolean includeCache, Boolean includePrivate) throws SQLException {
        try (Connection connection = getConnection()) {
            return getStorageSites(connection, includeCache, includePrivate);
        }
    }

    public Collection<StorageSite> getStorageSites(Connection connection, Boolean isCache, Boolean includePrivate) throws SQLException {
        try (Statement s = connection.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT storageSiteId, resourceURI, "
                    + "currentNum, currentSize, quotaNum, quotaSize, username, "
                    + "password, encrypt, private FROM storage_site_table "
                    + "JOIN credential_table ON credentialRef = credintialId "
                    + "WHERE isCache = " + isCache + " AND removing = FALSE")) {
                ArrayList<StorageSite> res = new ArrayList<>();
                while (rs.next()) {
                    StorageSite ss = new StorageSite();
                    ss.setStorageSiteId(rs.getLong(1));

                    ss.setResourceURI(rs.getString(2));
                    ss.setCurrentNum(rs.getLong(3));
                    ss.setCurrentSize(rs.getLong(4));
                    ss.setQuotaNum(rs.getLong(5));
                    ss.setQuotaSize(rs.getLong(6));
                    Credential c = new Credential();
                    c.setStorageSiteUsername(rs.getString(7));
                    c.setStorageSitePassword(rs.getString(8));
                    ss.setCredential(c);
                    ss.setEncrypt(rs.getBoolean(9));
                    ss.setCache(isCache);
                    if (rs.getBoolean(10) && includePrivate) {
                        res.add(ss);
                    } else if (!rs.getBoolean(10)) {
                        res.add(ss);
                    }
                }
                return res;
            }
        }
    }

    public LogicalData registerDirLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, accessDate, ttlSec)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, Constants.LOGICAL_FOLDER);
            preparedStatement.setString(4, entry.getName());
            preparedStatement.setTimestamp(5, new Timestamp(entry.getCreateDate()));
            preparedStatement.setTimestamp(6, new Timestamp(entry.getModifiedDate()));
            preparedStatement.setTimestamp(7, new Timestamp(entry.getLastAccessDate()));
            if (entry.getTtlSec() == null) {
                preparedStatement.setNull(8, Types.INTEGER);
            } else {
                preparedStatement.setInt(8, entry.getTtlSec());
            }

//            preparedStatement.setString(9, entry.getDataLocationPreference());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));

//            String path = getPathforLogicalData(entry, connection);
//            entry.setDataLocationPreferences(getDataLocationPreferace(connection, entry.getUid()));
            return entry;
        }
    }

    public LogicalData registerLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO ldata_table(parentRef, ownerId, datatype, "
                + "createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, "
                + "checksum, lastValidationDate, lockTokenId, "
                + "lockScope, lockType, lockedByUser, lockDepth, "
                + "lockTimeout, description, "
                + "ldName, accessDate, ttlSec) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, entry.getType());
            preparedStatement.setTimestamp(4, new Timestamp(entry.getCreateDate()));
            preparedStatement.setTimestamp(5, new Timestamp(entry.getModifiedDate()));
            preparedStatement.setLong(6, entry.getLength());
            preparedStatement.setString(7, entry.getContentTypesAsString());
            preparedStatement.setLong(8, entry.getPdriGroupId());
            preparedStatement.setBoolean(9, entry.getSupervised());
            preparedStatement.setString(10, entry.getChecksum());
            preparedStatement.setLong(11, entry.getLastValidationDate());
            preparedStatement.setString(12, entry.getLockTokenID());
            preparedStatement.setString(13, entry.getLockScope());
            preparedStatement.setString(14, entry.getLockType());
            preparedStatement.setString(15, entry.getLockedByUser());
            preparedStatement.setString(16, entry.getLockDepth());
            preparedStatement.setLong(17, entry.getLockTimeout());
            preparedStatement.setString(18, entry.getDescription());

//            preparedStatement.setString(19, entry.getDataLocationPreference());
            preparedStatement.setString(19, entry.getName());
            preparedStatement.setTimestamp(20, new Timestamp(entry.getLastAccessDate()));
            if (entry.getTtlSec() == null) {
                preparedStatement.setNull(21, Types.INTEGER);
            } else {
                preparedStatement.setInt(21, entry.getTtlSec());
            }
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
//            entry.setDataLocationPreferences(getDataLocationPreferace(connection, entry.getUid()));
            return entry;
        }
    }

    private LogicalData updateLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET modifiedDate = ?, "
                + "ldLength = ?, "
                + "contentTypesStr = ?, "
                + "pdriGroupRef = ?, "
                + "accessDate = ?, "
                + "ttlSec = ? "
                + "WHERE uid = ?")) {
            ps.setTimestamp(1, new Timestamp(entry.getModifiedDate()));
            ps.setLong(2, entry.getLength());
            ps.setString(3, entry.getContentTypesAsString());
            ps.setLong(4, entry.getPdriGroupId());
            if (entry.getLastAccessDate() == null) {
                ps.setNull(5, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(5, new Timestamp(entry.getLastAccessDate()));
            }
            if (entry.getTtlSec() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, entry.getTtlSec());
            }
            ps.setLong(7, entry.getUid());
            ps.executeUpdate();
//            entry.setDataLocationPreferences(getDataLocationPreferace(connection, entry.getUid()));
            return entry;
        }
    }

    public void updatePdri(LogicalData logicalData, PDRI pdri, @Nonnull Connection connection) throws SQLException {
        String sqlQuery;
        sqlQuery = "INSERT INTO pdri_table "
                + "(fileName, storageSiteRef, pdriGroupRef, isEncrypted) VALUES(?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
            preparedStatement.setString(1, pdri.getFileName());
            preparedStatement.setLong(2, pdri.getStorageSiteId());
            preparedStatement.setLong(3, logicalData.getPdriGroupId());
            preparedStatement.setBoolean(4, pdri.getEncrypted());
            preparedStatement.executeUpdate();
//                logicalData.setPdriGroupId(logicalData.getPdriGroupId());
//                return updateLogicalData(logicalData, connection);
            updateLogicalData(logicalData, connection);
        }
    }

    public LogicalData updateLogicalDataAndPdri(LogicalData logicalData, PDRI pdri, @Nonnull Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
            statement.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(0)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            Long newGroupId = rs.getLong(1);
            String sqlQuery;
//            if (pdri.getKeyInt() != null) {
            sqlQuery = "INSERT INTO pdri_table "
                    + "(fileName, storageSiteRef, pdriGroupRef, isEncrypted) VALUES(?, ?, ?, ?)";
//            } else {
//                sqlQuery = "INSERT INTO pdri_table "
//                        + "(fileName, storageSiteRef, pdriGroupRef, isEncrypted, encriptionKey) VALUES(?, ?, ?, ?, ?)";
//            }
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                preparedStatement.setString(1, pdri.getFileName());
                preparedStatement.setLong(2, pdri.getStorageSiteId());
                preparedStatement.setLong(3, newGroupId);
                preparedStatement.setBoolean(4, pdri.getEncrypted());
//                if (pdri.getKeyInt() != null) {
//                    //We have a triger to generate it 
//                    preparedStatement.setLong(5, pdri.getKeyInt().longValue());
//                }
                preparedStatement.executeUpdate();
                logicalData.setPdriGroupId(newGroupId);
                return updateLogicalData(logicalData, connection);
            }
        }
    }

    public LogicalData associateLogicalDataAndPdri(LogicalData logicalData, PDRI pdri, @Nonnull Connection connection) throws SQLException {
        connection = checkConnection(connection);
        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
            statement.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            Long newGroupId = rs.getLong(1);
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table (fileName, storageSiteRef, pdriGroupRef, isEncrypted) VALUES(?, ?, ?, ?)")) {
                preparedStatement.setString(1, pdri.getFileName());
                preparedStatement.setLong(2, pdri.getStorageSiteId());
                preparedStatement.setLong(3, newGroupId);
                preparedStatement.setBoolean(4, pdri.getEncrypted());
                preparedStatement.executeUpdate();
                logicalData.setPdriGroupId(newGroupId);
                return registerLogicalData(logicalData, connection);
            }
        }
    }

    public Long associateLogicalDataAndPdriGroup(LogicalData logicalData, @Nonnull Connection connection) throws SQLException {
        connection = checkConnection(connection);
        Long newGroupId;
        try (Statement statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_UPDATABLE)) {
            statement.executeUpdate("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            rs.next();
            newGroupId = rs.getLong(1);
        }
        logicalData.setPdriGroupId(newGroupId);
        registerLogicalData(logicalData, connection);
        return newGroupId;
    }

    public List<PDRIDescr> getPdriDescrByGroupId(Long groupId) throws SQLException, IOException {
        List<PDRIDescr> res = getFromPDRIDescrCache(groupId);
        if (res != null) {
            return res;
        }
        try (Connection connection = getConnection()) {
            return getPdriDescrByGroupId(groupId, connection);
        }
    }

    public List<PDRIDescr> getPdriStorageSiteID(Long StorageSiteId, @Nonnull Connection connection) throws SQLException {
        List<PDRIDescr> res = getFromPDRIDescrCache(StorageSiteId);
        if (res != null) {
            return res;
        }
        res = new ArrayList<>();
        long pdriId = -1;
        try (PreparedStatement ps = connection.prepareStatement(""
                + "SELECT fileName, storageSiteRef, storage_site_table.resourceUri, "
                + "username, password, isEncrypted, encryptionKey, pdri_table.pdriId, pdriGroupRef, storage_site_table.isCache "
                + "FROM pdri_table "
                + "JOIN storage_site_table ON storageSiteRef = storageSiteId "
                + "JOIN credential_table ON credentialRef = credintialId "
                + "WHERE storageSiteRef = ? ")) {
            ps.setLong(1, StorageSiteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String fileName = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                boolean encrypt = rs.getBoolean(6);
                long key = rs.getLong(7);
                pdriId = rs.getLong(8);
                boolean isCache = rs.getBoolean(9);
                //                if (resourceURI.startsWith("lfc") || resourceURI.startsWith("srm")
                //                        || resourceURI.startsWith("gftp")) {
                //                    try {
                //                        passwd = getProxyAsBase64String();
                //                    } catch (FileNotFoundException ex) {
                //                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
                //                    } catch (IOException ex) {
                //                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
                //                    }
                //                }
                long groupId = rs.getLong(9);
                res.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), groupId, (pdriId), isCache));
            }
            if (pdriId > -1) {
                putToPDRIDescrCache(pdriId, res);
            }
            return res;
        }
    }

    public List<PDRIDescr> getPdriDescrByGroupId(Long groupId, @Nonnull Connection connection) throws SQLException {
        List<PDRIDescr> res = getFromPDRIDescrCache(groupId);
        if (res != null) {
            return res;
        }
        res = new ArrayList<>();
        long pdriGroupRef;
        long pdriId;
        try (PreparedStatement ps = connection.prepareStatement("SELECT fileName, "
                + "storageSiteRef, storage_site_table.resourceUri, "
                + "username, password, isEncrypted, encryptionKey, pdri_table.pdriId, storage_site_table.isCache "
                + "FROM pdri_table "
                + "JOIN storage_site_table ON storageSiteRef = storageSiteId "
                + "JOIN credential_table ON credentialRef = credintialId "
                + "WHERE pdri_table.pdriGroupRef = ? ")) {
            ps.setLong(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String fileName = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                boolean encrypt = rs.getBoolean(6);
                long key = rs.getLong(7);
                pdriId = rs.getLong(8);
                boolean isCache = rs.getBoolean(9);
//                if (resourceURI.startsWith("lfc") || resourceURI.startsWith("srm")
//                        || resourceURI.startsWith("gftp")) {
//                    try {
//                        passwd = getProxyAsBase64String();
//                    } catch (FileNotFoundException ex) {
//                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (IOException ex) {
//                        Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
                res.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), Long.valueOf(groupId), Long.valueOf(pdriId), isCache));
            }
            putToPDRIDescrCache(groupId, res);
            return res;
        }
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName) throws SQLException, UnsupportedEncodingException {
        Path decodedLogicalFileName = Path.path(java.net.URLDecoder.decode(logicalResourceName.toString(), "UTF-8"));
        try (Connection connection = getConnection()) {
            LogicalData res = getLogicalDataByPath(decodedLogicalFileName, connection);
            return res;
        }
    }

    public Long getLogicalDataUidByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        Long res = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
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
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = " + parentRef + " AND ldata_table.ldName like '" + name + "'")) {
//            preparedStatement.setLong(1, parentRef);
//            preparedStatement.setString(2, name);
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
                + "lockTokenId, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                + "description, accessDate, ttlSec "
                //                + ", resourceUri "
                + "FROM ldata_table "
                //                + "JOIN pref_table ON ld_uid = uid "
                //                + "JOIN storage_site_table ON storageSiteId = storageSiteRef"
                + "WHERE ldata_table.parentRef = " + parentRef + " AND ldata_table.ldName like '" + name + "'")) {
//            preparedStatement.setLong(1, parentRef);
//            preparedStatement.setString(2, name);
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
                res.setChecksum(rs.getString(10));
                res.setLastValidationDate(rs.getLong(11));
                res.setLockTokenID(rs.getString(12));
                res.setLockScope(rs.getString(13));
                res.setLockType(rs.getString(14));
                res.setLockedByUser(rs.getString(15));
                res.setLockDepth(rs.getString(16));
                res.setLockTimeout(rs.getLong(17));
                res.setDescription(rs.getString(18));

//                res.setDataLocationPreference(rs.getString(19));
                Timestamp ts = rs.getTimestamp(19);
                res.setLastAccessDate(ts != null ? ts.getTime() : null);
                int ttl = rs.getInt(20);
                res.setTtlSec(rs.wasNull() ? null : ttl);
//                Array array = rs.getArray(21);

                return res;
            } else {
                return null;
            }

        }
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException, UnsupportedEncodingException {
        LogicalData res = null;
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
                            + "lockTokenId, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                            + "description, locationPreference, status, accessDate, ttlSec "
                            //                            + ", resourceUri "
                            + "FROM ldata_table "
                            //                            + "JOIN pref_table ON ld_uid = uid "
                            //                            + "JOIN storage_site_table ON storageSiteId = storageSiteRef "
                            + "WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            res = new LogicalData();
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
                            res.setChecksum(rs.getString(10));
                            res.setLastValidationDate(rs.getLong(11));
                            res.setLockTokenID(rs.getString(12));
                            res.setLockScope(rs.getString(13));
                            res.setLockType(rs.getString(14));
                            res.setLockedByUser(rs.getString(15));
                            res.setLockDepth(rs.getString(16));
                            res.setLockTimeout(rs.getLong(17));
                            res.setDescription(rs.getString(18));

//                            res.setDataLocationPreference(rs.getString(19));
                            res.setStatus(rs.getString(20));
                            Timestamp ts = rs.getTimestamp(21);
                            //Object ts = rs.getObject(21);
                            res.setLastAccessDate(ts != null ? ts.getTime() : null);
                            int ttl = rs.getInt(22);
                            res.setTtlSec(rs.wasNull() ? null : ttl);
//                            Array perf = rs.getArray(23);

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
        LogicalData res = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenId, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, "
                + "status, accessDate, ttlSec "
                //                + ", resourceUri "
                + "FROM ldata_table "
                //                + "JOIN pref_table ON ld_uid = uid "
                //                + "JOIN storage_site_table ON storageSiteId = storageSiteRef"
                + "WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = new LogicalData();
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
                res.setChecksum(rs.getString(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));

//                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(20));
                res.setLastAccessDate(rs.getTimestamp(21) != null ? rs.getTimestamp(21).getTime() : null);
                int ttl = rs.getInt(22);
                res.setTtlSec(rs.wasNull() ? null : ttl);
//                res.setDataLocationPreferences(getDataLocationPreferace(connection, res.getUid()));
//                Array pref = rs.getArray(24);
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
        connection = checkConnection(connection);
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
            ResultSet rs = s.executeQuery("SELECT permType, roleName FROM permission_table "
                    + "WHERE permission_table.ldUidRef = " + UID);
            Set<String> canRead = new HashSet<>();
            Set<String> canWrite = new HashSet<>();
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
            p.setLocalId(UID);
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
                + "lockTokenId, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                + "description, locationPreference, accessDate, ttlSec "
                + "FROM ldata_table WHERE ldata_table.parentRef = ?")) {
            preparedStatement.setLong(1, parentRef);
            ResultSet rs = preparedStatement.executeQuery();
            LinkedList<LogicalData> res = new LinkedList<>();
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
                element.setChecksum(rs.getString(11));
                element.setLastValidationDate(rs.getLong(12));
                element.setLockTokenID(rs.getString(13));
                element.setLockScope(rs.getString(14));
                element.setLockType(rs.getString(15));
                element.setLockedByUser(rs.getString(16));
                element.setLockDepth(rs.getString(17));
                element.setLockTimeout(rs.getLong(18));
                element.setDescription(rs.getString(19));

//                element.setDataLocationPreference(rs.getString(20));
                element.setLastAccessDate(rs.getTimestamp(21) != null ? rs.getTimestamp(21).getTime() : null);
                int ttl = rs.getInt(22);
                element.setTtlSec(rs.wasNull() ? null : ttl);
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
//            String query = "UPDATE ldata_table SET parentRef = " + newParent.getUid() + ", ldName like '" + newName + "' WHERE uid = " + toMove.getUid();
//            ps.executeUpdate(query);
        }
    }

    public void copyFolder(LogicalData toCopy, LogicalData newParent, String newName, MyPrincipal principal, Connection connection) throws SQLException {
        try {
            Permissions toCopyPerm = getPermissions(toCopy.getUid(), toCopy.getOwner(), connection);
            Permissions newParentPerm = getPermissions(newParent.getUid(), newParent.getOwner(), connection);
            Permissions permissionsForNew = new Permissions(principal, newParentPerm);
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
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyFile(LogicalData toCopy, LogicalData newParent, String newName, MyPrincipal principal, Connection connection) throws SQLException {
        try {
            Permissions toCopyPerm = getPermissions(toCopy.getUid(), toCopy.getOwner(), connection);
            Permissions newParentPerm = getPermissions(newParent.getUid(), newParent.getOwner(), connection);
            Permissions permissionsForNew = new Permissions(principal, newParentPerm);
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
                    s.executeUpdate("UPDATE pdrigroup_table SET refCount=refCount+1 WHERE pdriGroupId = " + newFileEntry.getPdriGroupId());
                }
            }
        } catch (CloneNotSupportedException cns) {
            throw new RuntimeException(cns);
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

    public void setFileChecksum(@Nonnull Long uid, @Nonnull String checksum, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET checksum = ? WHERE uid = ?")) {
            ps.setString(1, checksum);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
    }

    public void setDriStatus(@Nonnull Long uid, @Nonnull String status, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET status = ? WHERE uid = ?")) {
            ps.setString(1, status);
            ps.setLong(2, uid);
            ps.executeUpdate();
        }
//        LogicalData cached = getFromLDataCache(uid);
//        if (cached != null) {
//        addToLDataCache( cached);
//        }
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

    public void setLocationPreferences(Connection connection, Long uid, List<String> locationPreferences, Boolean includePrivate) throws SQLException {
        if (locationPreferences != null && !locationPreferences.isEmpty()) {
            String storageSitesQuery = "select storageSiteId,resourceUri,private from storage_site_table "
                    + "WHERE resourceUri LIKE "; //use = 
            for (String site : locationPreferences) {
                try {
                    new VRL(site);
                    storageSitesQuery += "'" + site + "' OR resourceUri LIKE ";
                } catch (VRLSyntaxException ex) {
                    Logger.getLogger(JDBCatalogue.class.getName()).log(Level.WARNING, "Wrong VRL", ex);
                }
            }
            if (storageSitesQuery.endsWith(" OR resourceUri LIKE ")) {
                storageSitesQuery = storageSitesQuery.substring(0, storageSitesQuery.lastIndexOf("OR")) + "";
            }
            storageSitesQuery += " AND isCache = false";
            List<Long> ids = new ArrayList<>();
            try (Statement s = connection.createStatement()) {
                try (ResultSet rs = s.executeQuery(storageSitesQuery)) {
                    while (rs.next()) {
                        if (rs.getBoolean(3) && includePrivate) {
                            ids.add(rs.getLong(1));
                        } else if (!rs.getBoolean(3)) {
                            ids.add(rs.getLong(1));
                        }
                    }
                }
                if (!ids.isEmpty()) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "DELETE FROM pref_table WHERE ld_uid = ?")) {
                        preparedStatement.setLong(1, uid);
                        preparedStatement.executeUpdate();
                    }
//                connection.commit();
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO pref_table (ld_uid, storageSiteRef) VALUES(?,?)")) {
                        for (Long id : ids) {
                            preparedStatement.setLong(1, uid);
                            preparedStatement.setLong(2, id);
                            preparedStatement.addBatch();
//                        preparedStatement.executeUpdate();
                        }
                        preparedStatement.executeBatch();
                    }
                    Collection<LogicalData> children = getChildrenByParentRef(uid);
                    for (LogicalData child : children) {
                        if (!child.isFolder()) {
                            setLocationPreferences(connection, child.getUid(), locationPreferences, includePrivate);
                        }
                    }
                    setNeedsCheck(uid, connection);
                }
            }

        }
        connection.commit();
//        return locationPreferences;//getDataLocationPreferace(connection, uid);
    }

//    public List<String> setLocationPreferences(Connection connection, Long uid, List<String> locationPreferences, Boolean includePrivate) throws SQLException {
//        List<String> dataPref = getDataLocationPreferace(connection, uid);
//
//        if (dataPref != null && !dataPref.isEmpty() && locationPreferences != null && !locationPreferences.isEmpty()) {
//            locationPreferences.removeAll(dataPref);
//        }
//
//        String storageSitesQuery = "select storageSiteId,resourceUri,private from storage_site_table "
//                + "WHERE resourceUri LIKE ";
//        List<String> sites = new ArrayList<>();
//
//        for (String site : locationPreferences) {
//            try {
//                new VRL(site);
//                storageSitesQuery += "'" + site + "' OR resourceUri LIKE ";
//            } catch (VRLSyntaxException ex) {
//                Logger.getLogger(JDBCatalogue.class.getName()).log(Level.WARNING, "Wrong VRL", ex);
//            }
//        }
//        if (storageSitesQuery.endsWith(" OR resourceUri LIKE ")) {
//            storageSitesQuery = storageSitesQuery.substring(0, storageSitesQuery.lastIndexOf("OR")) + "";
//        }
//        if (locationPreferences != null && !locationPreferences.isEmpty()) {
//            storageSitesQuery += " AND isCache = false";
//            List<Long> ids = new ArrayList<>();
//            try (Statement s = connection.createStatement()) {
//                try (ResultSet rs = s.executeQuery(storageSitesQuery)) {
//                    while (rs.next()) {
//                        if (rs.getBoolean(3) && includePrivate) {
//                            sites.add(rs.getString(2));
//                            ids.add(rs.getLong(1));
//                        } else if (!rs.getBoolean(3)) {
//                            sites.add(rs.getString(2));
//                            ids.add(rs.getLong(1));
//                        }
//                    }
//                }
//                for (Long id : ids) {
//                    String query = "INSERT INTO pref_table (ld_uid, storageSiteRef) VALUES ('"
//                            + uid + "', '" + id + "')";
//                    s.addBatch(query);
//                }
//                s.executeBatch();
//            }
//        }
//        LogicalData ldata = getFromLDataCache(uid, null);
//        if (ldata != null) {
//            ldata.setDataLocationPreferences(dataPref);
//            putToLDataCache(ldata, null);
//        }
//        connection.commit();
//        return getDataLocationPreferace(connection, uid);
//    }
    public void updatePdris(List<PDRIDescr> pdrisToUpdate, Connection connection) throws SQLException {
        for (PDRIDescr d : pdrisToUpdate) {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE pdri_table SET isEncrypted = ?, storageSiteRef = ? WHERE pdriId = ?")) {
                ps.setBoolean(1, d.getEncrypt());
                ps.setLong(2, d.getStorageSiteId());
                ps.setLong(3, d.getId());
                ps.executeUpdate();
                List<PDRIDescr> res = getPdriDescrByGroupId(d.getPdriGroupRef(), connection);
                putToPDRIDescrCache(d.getPdriGroupRef(), res);
            }
        }
    }

    public void updateStorageSites(HashMap<String, Boolean> hostEncryptMap, Connection connection) throws SQLException, URISyntaxException {
        Set<String> keys = hostEncryptMap.keySet();
        StringBuilder updateTrue = new StringBuilder();
        updateTrue.append("UPDATE storage_site_table SET encrypt = TRUE WHERE");

        StringBuilder updateFalse = new StringBuilder();
        updateFalse.append("UPDATE storage_site_table SET encrypt = FALSE WHERE");
        boolean doUpdateTrue = false;
        boolean doUpdateFalse = false;
        for (String k : keys) {
            URI uri = new URI(k);
            if (hostEncryptMap.get(k)) {
                doUpdateTrue = true;
                updateTrue.append("(resourceUri LIKE ").append("'").append(uri.getScheme()).append("://%").append(uri.getHost()).append("%') ");
                updateTrue.append(" OR ");
            } else {
                doUpdateFalse = true;
                updateFalse.append("(resourceUri LIKE ").append("'").append(uri.getScheme()).append("://%").append(uri.getHost()).append("%') ");
                updateFalse.append(" OR ");
            }
        }
        if (doUpdateFalse) {
            updateFalse.replace(updateFalse.lastIndexOf("OR"), updateFalse.length(), "");

            try (PreparedStatement ps = connection.prepareStatement(updateFalse.toString())) {
                ps.executeUpdate();
            }
        }
        if (doUpdateTrue) {
            updateTrue.replace(updateTrue.lastIndexOf("OR"), updateTrue.length(), "");
            try (PreparedStatement ps = connection.prepareStatement(updateTrue.toString())) {
                ps.executeUpdate();
            }
        }
    }

//    public void recordRequest(Connection connection, HttpServletRequest httpServletRequest, double elapsed) throws SQLException, UnsupportedEncodingException {
//        try (PreparedStatement preparedStatement = connection.prepareStatement(
//                "INSERT INTO requests_table (methodName, requestURL, "
//                + "remoteAddr, contentLen, contentType, elapsedTime,userName, userAgent) "
//                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
//            preparedStatement.setString(1, httpServletRequest.getMethod());
//            preparedStatement.setString(2, httpServletRequest.getRequestURL().toString());
//            preparedStatement.setString(3, httpServletRequest.getRemoteAddr());
//            preparedStatement.setInt(4, httpServletRequest.getContentLength());
//            preparedStatement.setString(5, httpServletRequest.getContentType());
//            preparedStatement.setDouble(6, elapsed);
//
//
//            String userNpasswd = getUserName(httpServletRequest);
//            preparedStatement.setString(7, userNpasswd);
//            preparedStatement.setString(8, httpServletRequest.getHeader("User-Agent"));
//            preparedStatement.executeUpdate();
//            ResultSet rs = preparedStatement.getGeneratedKeys();
//        }
//    }
    public void recordRequests(Connection connection, List<RequestWapper> requestEvents) throws SQLException, UnsupportedEncodingException {
        try (Statement s = connection.createStatement()) {
            for (RequestWapper e : requestEvents) {
                String query = "INSERT INTO requests_table (methodName, requestURL, "
                        + "remoteAddr, contentLen, contentType, elapsedTime,userName, timeStamp ,userAgent) "
                        + "VALUES('" + e.getMethod() + "', '" + e.getRequestURL()
                        + "', '" + e.getRemoteAddr() + "', '" + e.getContentLength()
                        + "', '" + e.getContentType() + "',  '" + e.getElapsed()
                        + "', '" + e.getUserNpasswd() + "',  '" + new Timestamp(e.getTimeStamp())
                        + "', '" + e.getUserAgent() + "')";
                s.addBatch(query);
            }
            s.executeBatch();
        }
    }

    public void insertOrUpdateStorageSites(Collection<StorageSite> sites, Connection connection, Boolean includePrivate) throws SQLException {
        Collection<Credential> credentials = getCredentials(connection);
        Collection<StorageSite> existingSites = getStorageSites(connection, Boolean.FALSE, includePrivate);
        existingSites.addAll(getStorageSites(connection, Boolean.TRUE, includePrivate));
        Collection<String> updatedSites = new ArrayList<>();

        for (StorageSite s : sites) {
            long credentialID = -1;
            for (Credential c : credentials) {
                if (c.getStorageSiteUsername().equals(s.getCredential().getStorageSiteUsername())
                        && c.getStorageSitePassword().equals(s.getCredential().getStorageSitePassword())) {
                    credentialID = c.getId();
                    break;
                }
            }

            if (credentialID == -1) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO credential_table (username, "
                        + "password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, s.getCredential().getStorageSiteUsername());
                    preparedStatement.setString(2, s.getCredential().getStorageSitePassword());
                    preparedStatement.executeUpdate();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                    credentialID = rs.getLong(1);
                    s.getCredential().setId(credentialID);
                    credentials.add(s.getCredential());
                }
            }

            for (StorageSite es : existingSites) {
                if (es.getResourceURI().equals(s.getResourceURI())) {
//                    Long id = es.getStorageSiteId();
                    try (PreparedStatement preparedStatement = connection.prepareStatement(
                            "UPDATE storage_site_table SET "
                            + "resourceUri = ?, "
                            + "credentialRef = ?, "
                            + "currentNum = ?, "
                            + "currentSize = ?, "
                            + "quotaNum = ?, "
                            + "quotaSize = ?, "
                            + "isCache = ?, "
                            + "encrypt = ? "
                            + "WHERE storageSiteId = ?")) {
                        preparedStatement.setString(1, s.getResourceURI());
                        preparedStatement.setLong(2, credentialID);
                        preparedStatement.setLong(3, s.getCurrentNum());
                        preparedStatement.setLong(4, s.getCurrentSize());
                        preparedStatement.setLong(5, s.getQuotaNum());
                        preparedStatement.setLong(6, s.getQuotaSize());
                        preparedStatement.setBoolean(7, s.isCache());
                        preparedStatement.setBoolean(8, s.isEncrypt());
                        preparedStatement.setLong(9, es.getStorageSiteId());
                        preparedStatement.executeUpdate();
                    }
                    updatedSites.add(es.getResourceURI());
                }
            }
            if (!updatedSites.contains(s.getResourceURI())) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO storage_site_table "
                        + "(resourceUri, credentialRef, currentNum, "
                        + "currentSize, quotaNum, quotaSize, isCache, extra, "
                        + "encrypt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, s.getResourceURI());
                    preparedStatement.setLong(2, credentialID);
                    preparedStatement.setLong(3, s.getCurrentNum());
                    preparedStatement.setLong(4, s.getCurrentSize());
                    preparedStatement.setLong(5, s.getQuotaNum());
                    preparedStatement.setLong(6, s.getQuotaSize());
                    preparedStatement.setBoolean(7, s.isCache());
                    preparedStatement.setBoolean(8, s.isEncrypt());
                    preparedStatement.executeUpdate();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                    s.setStorageSiteId(rs.getLong(1));
                    existingSites.add(s);
                }
            }

        }
    }

    public void deleteStorageSites(List<Long> ids, Connection connection) throws SQLException {
        if (ids != null && ids.size() > 0) {
            for (Long id : ids) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "DELETE FROM storage_site_table WHERE storageSiteId = ?")) {
                    preparedStatement.setLong(1, id);
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    private Collection<Credential> getCredentials(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "select * from credential_table")) {

            ResultSet rs = preparedStatement.executeQuery();
            Collection<Credential> res = new ArrayList<>();
            while (rs.next()) {
                Credential c = new Credential();
                c.setId(rs.getLong(1));
                c.setStorageSiteUsername(rs.getString(2));
                c.setStorageSitePassword(rs.getString(3));
                res.add(c);
            }
            return res;
        }
    }

    public List<UsersWrapper> getUsers(Connection connection) throws SQLException {
        List<UsersWrapper> users = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "select * from auth_usernames_table")) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                UsersWrapper uw = new UsersWrapper();
                long id = rs.getLong(1);
                uw.setId(id);
                String token = rs.getString(2);
//                token = "****";
                uw.setToken(token);
                uw.setUname(rs.getString(3));

                List<String> roles = new ArrayList<>();
                try (PreparedStatement preparedStatement2 = connection.prepareStatement(
                        "SELECT roleName FROM auth_roles_tables WHERE unameRef = " + id)) {
                    ResultSet rs2 = preparedStatement2.executeQuery();
                    while (rs2.next()) {
                        roles.add(rs2.getString(1));
                    }
                }
                uw.setRoles(roles);
                users.add(uw);
            }
        }
        return users;
    }

    public List<LogicalData> getLogicalDataByName(Path fileName, Connection connection) throws SQLException, UnsupportedEncodingException {
        Path decodedLogicalFileName = Path.path(java.net.URLDecoder.decode(fileName.toString(), "UTF-8"));
        try (PreparedStatement ps = connection.prepareStatement("SELECT uid, parentRef, "
                + "ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, checksum, "
                + "lastValidationDate, lockTokenID, lockScope, lockType, "
                + "lockedByUser, lockDepth, lockTimeout, description, "
                + "locationPreference, status"
                //                + ", resourceUri "
                + "FROM ldata_table "
                //                + "JOIN pref_table ON ld_uid = uid "
                //                + "JOIN storage_site_table ON storageSiteId = storageSiteRef"
                + "WHERE ldata_table.ldName like '" + decodedLogicalFileName.toString() + "'")) {

//            ps.setString(1, fileName.toString());
            ResultSet rs = ps.executeQuery();
            List<LogicalData> results = new ArrayList<>();
            while (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(rs.getLong(1));
                res.setParentRef(rs.getLong(2));
                res.setOwner(rs.getString(3));
                res.setType(rs.getString(4));
                res.setName(rs.getString(5));
                res.setCreateDate(rs.getTimestamp(6).getTime());
                res.setModifiedDate(rs.getTimestamp(7).getTime());
                res.setLength(rs.getLong(8));
                res.setContentTypesAsString(rs.getString(9));
                res.setPdriGroupId(rs.getLong(10));
                res.setSupervised(rs.getBoolean(11));
                res.setChecksum(rs.getString(12));
                res.setLastValidationDate(rs.getLong(13));
                res.setLockTokenID(rs.getString(14));
                res.setLockScope(rs.getString(15));
                res.setLockType(rs.getString(16));
                res.setLockedByUser(rs.getString(17));
                res.setLockDepth(rs.getString(18));
                res.setLockTimeout(rs.getLong(19));
                res.setDescription(rs.getString(20));
//                res.setDataLocationPreference(rs.getString(21));
                res.setStatus(rs.getString(22));
                //                Array locationPerf = rs.getArray(23);
//                res.setDataLocationPreferences(getDataLocationPreferace(connection, res.getUid()));
                results.add(res);
            }
            return results;
        }
    }

//    private String getUserName(HttpServletRequest httpServletRequest) throws UnsupportedEncodingException {
//        String authorizationHeader = httpServletRequest.getHeader("authorization");
//        String userNpasswd = "";
//        if (authorizationHeader != null) {
//            final int index = authorizationHeader.indexOf(' ');
//            if (index > 0) {
//                final String credentials = new String(Base64.decodeBase64(authorizationHeader.substring(index).getBytes()), "UTF8");
//                String[] encodedToken = credentials.split(":");
//                if (encodedToken.length > 1) {
//                    String token = new String(Base64.decodeBase64(encodedToken[1]));
//                    if (token.contains(";") && token.contains("uid=")) {
//                        String uid = token.split(";")[0];
//                        userNpasswd = uid.split("uid=")[1];
//                    } else {
//                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
//                    }
//                }
////                    if (userNpasswd == null || userNpasswd.length() < 1) {
////                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
////                    }
//
////                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");
//
////                final String token = credentials.substring(credentials.indexOf(":") + 1);
//            }
//        }
//        return userNpasswd;
//    }
    public void setSpeed(Stats stats) throws SQLException {
        try (Connection connection = getConnection()) {
            try {
                connection.setAutoCommit(false);
                setSpeed(stats, connection);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void setSpeed(Stats stats, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "select id, averageSpeed, minSpeed, maxSpeed, offlineCount from speed_table "
                + "where src = ? AND dst = ? AND fSize = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            ps.setString(1, stats.getSource());
            ps.setString(2, stats.getDestination());
            String size = "m";
            if (stats.getSize() < 2097152) {
                size = "s";
            } else if (stats.getSize() >= 2097152 && stats.getSize() < 20971520) {
                size = "m";
            } else if (stats.getSize() >= 20971520 && stats.getSize() < 209715200) {
                size = "l";
            } else if (stats.getSize() >= 209715200) {
                size = "xl";
            }
            ps.setString(3, size);
            ResultSet rs = ps.executeQuery();
            int id = -1;
            double averageSpeed = -1;
            double minSpeed = -1;
            double maxSpeed = -1;
            while (rs.next()) {
                id = rs.getInt(1);
                averageSpeed = rs.getDouble(2);
                minSpeed = rs.getDouble(3);
                maxSpeed = rs.getDouble(4);
                int offLineCount = rs.getInt(5);
                double a = 0.7;
                averageSpeed = a * averageSpeed + (1 - a) * stats.getSpeed();
                if (stats.getSpeed() > maxSpeed) {
                    maxSpeed = stats.getSpeed();
                }
                if (stats.getSpeed() < minSpeed) {
                    minSpeed = stats.getSpeed();
                }
                rs.updateDouble(2, averageSpeed);
                rs.updateDouble(3, minSpeed);
                rs.updateDouble(4, maxSpeed);
                rs.updateRow();
                connection.commit();
            }
            if (id == -1) {
                averageSpeed = stats.getSpeed();
                maxSpeed = stats.getSpeed();
                minSpeed = stats.getSpeed();
                try (PreparedStatement ps3 = connection.prepareStatement("INSERT "
                        + "INTO speed_table (src, dst, fSize, averageSpeed, minSpeed, maxSpeed) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps3.setString(1, stats.getSource());
                    ps3.setString(2, stats.getDestination());
                    ps3.setString(3, size);
                    ps3.setDouble(4, averageSpeed);
                    ps3.setDouble(5, minSpeed);
                    ps3.setDouble(6, maxSpeed);
                    ps3.executeUpdate();
                }
            }
        }
    }

    public void setTTL(Long uid, Integer ttl) throws SQLException {
        try (Connection cn = getConnection()) {
            setTTL(uid, ttl, cn);
        }
    }

    public void setTTL(Long uid, Integer ttl, Connection cn) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT uid, ownerId, "
                + "ttlSec FROM ldata_table WHERE uid=?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            ps.setLong(1, uid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            } else {
                rs.updateInt(3, ttl);
                rs.updateRow();
                cn.commit();
            }
        }
    }

    private void setNeedsCheck(Long logicalDataUID, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE pdrigroup_table JOIN ldata_table ON ldata_table.uid = ? SET needCheck = true WHERE pdriGroupId = ldata_table.pdriGroupRef")) {
            ps.setLong(1, logicalDataUID);
            ps.executeUpdate();
        }
    }

    public int getReplicationQueueLen() throws SQLException {
        try (Connection connection = getConnection()) {
            return getReplicationQueueLen(connection);
        }

    }

    public int getReplicationQueueLen(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT count(uid)"
                + "FROM pdri_table "
                + "JOIN ldata_table ON pdri_table.pdriGroupRef = ldata_table.pdriGroupRef "
                + "JOIN storage_site_table ON storage_site_table.storageSiteId = pdri_table.storageSiteRef "
                + "WHERE isCache = true")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } finally {
            connection.close();
        }
        return -1;
    }

    public List<LogicalData> getReplicationQueue() throws SQLException {
        try (Connection connection = getConnection()) {
            return getReplicationQueue(connection);
        }
    }

    public List<LogicalData> getReplicationQueue(Connection connection) throws SQLException {
        List<LogicalData> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT uid, parentRef, "
                + "ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdri_table.pdriGroupRef, isSupervised, checksum, "
                + "lastValidationDate, lockTokenID, lockScope, lockType, lockedByUser, "
                + "lockDepth, lockTimeout, description, locationPreference, status, accessDate, ttlSec "
                + "FROM pdri_table "
                + "JOIN ldata_table ON pdri_table.pdriGroupRef = ldata_table.pdriGroupRef "
                + "JOIN storage_site_table ON storage_site_table.storageSiteId = pdri_table.storageSiteRef "
                + "WHERE isCache = true")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(rs.getLong(1));
                res.setParentRef(rs.getLong(2));
                res.setOwner(rs.getString(3));
                res.setType(rs.getString(4));
                res.setName(rs.getString(5));
                res.setCreateDate(rs.getTimestamp(6).getTime());
                res.setModifiedDate(rs.getTimestamp(7).getTime());
                res.setLength(rs.getLong(8));
                res.setContentTypesAsString(rs.getString(9));
                res.setPdriGroupId(rs.getLong(10));
                res.setSupervised(rs.getBoolean(11));
                res.setChecksum(rs.getString(12));
                res.setLastValidationDate(rs.getLong(13));
                res.setLockTokenID(rs.getString(14));
                res.setLockScope(rs.getString(15));
                res.setLockType(rs.getString(16));
                res.setLockedByUser(rs.getString(17));
                res.setLockDepth(rs.getString(18));
                res.setLockTimeout(rs.getLong(19));
                res.setDescription(rs.getString(20));
                res.setStatus(rs.getString(21));
                res.setLastAccessDate(rs.getTimestamp(22) != null ? rs.getTimestamp(22).getTime() : null);
                int ttl = rs.getInt(23);
                res.setTtlSec(rs.wasNull() ? null : ttl);
                list.add(res);
            }

        }
        return list;
    }

    public Long getReplicationQueueSize(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT sum(ldLength) "
                + "FROM pdri_table "
                + "JOIN ldata_table ON pdri_table.pdriGroupRef = ldata_table.pdriGroupRef "
                + "JOIN storage_site_table ON storage_site_table.storageSiteId = pdri_table.storageSiteRef "
                + "WHERE isCache = true")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } finally {
//            connection.close();
        }
        return null;
    }

    public Long getReplicationQueueSize() throws SQLException {
        try (Connection connection = getConnection()) {
            return getReplicationQueueSize(connection);
        }
    }

    private Connection checkConnection(Connection connection) throws SQLException {
        if (connection == null || connection.isClosed()) {
            return getConnection();
        } else {
            return connection;
        }
    }

    public static List<PDRIDescr> getFromPDRIDescrCache(Long groupId) {
        if (usePDRIDescrCache) {
            List<PDRIDescr> pdr = PDRIDescrCache.get(groupId);
            if (PDRIDescrCache.size() > cacheSize) {
                PDRIDescrCache.remove(PDRIDescrCache.keySet().iterator().next());
            }
            return pdr;
        }
        return null;
    }

    public static void putToPDRIDescrCache(long pdriId, List<PDRIDescr> res) {
        if (usePDRIDescrCache) {
            if (PDRIDescrCache.size() > cacheSize) {
                PDRIDescrCache.remove(PDRIDescrCache.keySet().iterator().next());
            }
            PDRIDescrCache.put(pdriId, res);
        }
    }

    public static void removeFromPDRIDescrCache(Long id) {
        if (usePDRIDescrCache) {
            PDRIDescrCache.remove(id);
        }

    }

    public class PathInfo {

        private String name;
        private Long parentRef;

        private PathInfo(String name, Long parentRef) {
            this.name = name;
            this.parentRef = parentRef;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the parentRef
         */
        public Long getParentRef() {
            return parentRef;
        }
    }

    private void getPathforLogicalData(PathInfo pi, List<PathInfo> pil, PreparedStatement ps) throws SQLException {
        pil.add(pi);
        if (pi != null && pi.getParentRef() != 1) {
            ps.setLong(1, pi.getParentRef());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pi = new PathInfo(rs.getString(1), rs.getLong(2));
                    getPathforLogicalData(pi, pil, ps);
                }
            }
        }
    }

    public String getPathforLogicalData(LogicalData ld) throws SQLException {
        try (Connection connection = getConnection()) {
            String res = getPathforLogicalData(ld, connection);
            return res;
        }
    }

    public String getPathforLogicalData(LogicalData ld, @Nonnull Connection connection) throws SQLException {
        String res = null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ldName, parentRef FROM ldata_table WHERE uid = ?")) {
            PathInfo pi = new PathInfo(ld.getName(), ld.getParentRef());
            List<PathInfo> pil = new ArrayList<>();
            getPathforLogicalData(pi, pil, ps);
            res = "";
            Collections.reverse(pil);
            for (PathInfo pi1 : pil) {
//                System.err.println("'" + pi1.getName() + "'");
                res = res + "/" + pi1.getName();
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

    public void updateAccessTime(@Nonnull Long uid) throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE ldata_table SET accessDate = ? WHERE uid = ?")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setLong(2, uid);
                ps.executeUpdate();
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void addViewForRes(@Nonnull Long uid, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE wp4_table SET views = views + 1, need_update = TRUE WHERE local_id = ?")) {
            ps.setLong(1, uid);
            ps.executeUpdate();
        }
    }

    public void addViewForRes(@Nonnull Long uid) {
        try {
            try (Connection connection = getConnection()) {
                try {
                    addViewForRes(uid, connection);
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            Logger.getLogger(JDBCatalogue.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public String getGlobalID(Long uid, Connection connection) throws SQLException {
        String res = null;

        //        String res = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT global_id FROM wp4_table WHERE local_id = ?")) {
            ps.setLong(1, uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = rs.getString(1);
            }

        }

        return res;
    }

    public String getGlobalID(Long uid) throws SQLException {
        try (Connection connection = getConnection()) {
            return getGlobalID(uid, connection);
        }
    }

    public void setPreferencesOn(Long uidTo, Long uidFrom, Connection connection) throws SQLException {
        try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM pref_table WHERE ld_uid = ?");
                PreparedStatement psIns = connection.prepareStatement("INSERT "
                        + "INTO pref_table (ld_uid, storageSiteRef) "
                        + "SELECT ?, storageSiteRef FROM pref_table WHERE ld_uid=?")) {
            psDel.setLong(1, uidTo);
            psIns.setLong(1, uidTo);
            psIns.setLong(2, uidFrom);
            psDel.executeUpdate();
            psIns.executeUpdate();
        }
    }
}

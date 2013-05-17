/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import io.milton.common.Path;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.MyStorageSite;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.VRS;

/**
 *
 * @author S. Koulouzis
 */
public class Assimilator {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/lobcderDB";
    //  Database credentials
    static final String USER = "lobcder";
    static final String PASS = "RoomC3156";
    private final Connection conn;

    public Assimilator() throws ClassNotFoundException, SQLException {
        //STEP 2: Register JDBC driver
        Class.forName("com.mysql.jdbc.Driver");

        //STEP 3: Open a connection
        System.out.println("Connecting to database...");
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.setAutoCommit(false);
    }

    private long addCredentials(Connection connection, String username, String password) throws SQLException {
        long id;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                        + "credential_table (username, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getLong(1);
            System.out.println("ID: " + id);
        }
//        }
        return id;
    }

    private Connection getConnection() throws SQLException {
        return conn;
    }

    private long addStorageSite(Connection connection, MyStorageSite site, long credentialRef, boolean isCache) throws SQLException {
        long ssID;
        String uri;
        if (!site.getResourceURI().endsWith("/")) {
            uri = site.getResourceURI() + "/";
        } else {
            uri = site.getResourceURI();
        }
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                        + "storage_site_table (resourceUri, credentialRef, "
                        + "currentNum, currentSize, quotaNum, quotaSize, "
                        + "isCache, encrypt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, uri);
            ps.setLong(2, credentialRef);
            ps.setLong(3, site.getCurrentNum());
            ps.setLong(4, site.getCurrentSize());
            ps.setLong(5, site.getQuotaNum());
            ps.setLong(6, site.getQuotaSize());
            ps.setBoolean(7, isCache);
            ps.setBoolean(8, site.isEncrypt());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ssID = rs.getLong(1);
        }
        return ssID;
    }

    private long addPdrigroupTable(Connection connection) throws SQLException {
        long pdriGroupID;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            pdriGroupID = rs.getLong(1);
//                connection.commit();
            System.out.println("pdriGroupID: " + pdriGroupID);
        }
//        }
        return pdriGroupID;
    }

    private long addPDRI(Connection connection, String fileName, long storageSiteRef, long pdriGroupRef, boolean isEncrypted, BigInteger encryptionKey) throws SQLException {
        long pdriID;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO pdri_table "
                        + "(fileName, storageSiteRef, pdriGroupRef, isEncrypted, encryptionKey) "
                        + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fileName);
            ps.setLong(2, storageSiteRef);
            ps.setLong(3, pdriGroupRef);
            ps.setBoolean(4, isEncrypted);
            ps.setLong(5, encryptionKey.longValue());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            pdriID = rs.getLong(1);
//                connection.commit();
            System.out.println("pdriID: " + pdriID);
        }
//        }
        return pdriID;
    }

    private LogicalData addLogicalData(Connection connection, LogicalData entry) throws SQLException {

//        try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO ldata_table(parentRef, ownerId, datatype, "
                        + "createDate, modifiedDate, ldLength, "
                        + "contentTypesStr, pdriGroupRef, isSupervised, "
                        + "checksum, lastValidationDate, lockTokenId, "
                        + "lockScope, lockType, lockedByUser, lockDepth, "
                        + "lockTimeout, description, locationPreference, "
                        + "ldName) "
                        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, entry.getType());
            preparedStatement.setDate(4, new java.sql.Date(entry.getCreateDate()));
            preparedStatement.setDate(5, new java.sql.Date(entry.getModifiedDate()));
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
        } catch (SQLException ex) {
            if (ex instanceof MySQLIntegrityConstraintViolationException || ex.getMessage().contains("Duplicate entry")) {
                System.err.println(entry.getName() + " already exists!");
            } else {
                throw ex;
            }
        }
        return null;
//        }
    }

    private long getStorageSiteID(Connection connection, String ssURI) throws SQLException {
        long ssID = -1;
        String query = "select storageSiteId from storage_site_table where resourceUri = '"
                + ssURI + "' and isCache = false";
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        query)) {
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                ssID = rs.getLong(1);
            }
        }
        if (ssID == -1) {
            if (!ssURI.endsWith("/")) {
                ssURI += "/";
                query = "select storageSiteId from storage_site_table where resourceUri = '"
                        + ssURI + "' and isCache = false";
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                                query)) {
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        ssID = rs.getLong(1);
                    }
                }
            }
        }
        return ssID;
    }

    private void assimilate(List<MyStorageSite> sites) throws SQLException,
            MalformedURLException, VlException, NoSuchAlgorithmException {
        Connection c = getConnection();
        StorageSiteClient ssClient;
        for (MyStorageSite site : sites) {
            String username = site.getCredential().getStorageSiteUsername();
            String password = site.getCredential().getStorageSitePassword();
            String ssURI = site.getResourceURI();

            long ssID = getStorageSiteID(c, ssURI);
            if (ssID == -1) {
                long credentialsID = addCredentials(c, username, password);
                ssID = addStorageSite(c, site, credentialsID, false);
            }

            ssClient = new StorageSiteClient(username, password, ssURI);
            VDir dir = ssClient.getStorageSiteClient().openDir(new VRL(ssURI));
            //build folders first 
            add(dir, dir.getPath(), c, ssID, false);

            add(dir, dir.getPath(), c, ssID, true);

//            VFSNode[] nodes = dir.list();
//            for (VFSNode n : nodes) {
//                if (n.isFile()) {
//                    VFile f = (VFile) n;
//                    String fileName = n.getName();
//                    LogicalData registered = getLogicalDataByParentRefAndName(parentRef, fileName, c);
//                    VRL currentPath = new VRL(f.getPath().replaceFirst(dir.getPath(), ""));
//                    LogicalData parent = getLogicalDataByPath(Path.path(currentPath.getPath()), c);
//                    if (registered == null) {
//                        addFile(c, f, parentRef, parent.getUid());
//                    }
//                }
//            }
        }
        c.commit();
        c.close();
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

    public LogicalData registerDirLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate)"
                        + " VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, Constants.LOGICAL_FOLDER);
            preparedStatement.setString(4, entry.getName());
            preparedStatement.setDate(5, new java.sql.Date(entry.getCreateDate()));
            preparedStatement.setDate(6, new java.sql.Date(entry.getModifiedDate()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
            return entry;
        }
    }

    public void add(VDir dir, String base, Connection connection, long ssid, boolean addFiles) throws MalformedURLException, VlException, SQLException, NoSuchAlgorithmException {
        VFSNode[] nodes = dir.list();
        for (VFSNode f : nodes) {
            VRL currentPath = new VRL(f.getPath().replaceFirst(base, ""));
            LogicalData register = getLogicalDataByPath(Path.path(currentPath.getPath()), connection);
            LogicalData parent = getLogicalDataByPath(Path.path(currentPath.getPath()).getParent(), connection);
            if (f.isDir()) {
                if (register == null) {
                    LogicalData entry = new LogicalData();
                    entry.setCreateDate(f.getModificationTime());
                    entry.setModifiedDate(f.getModificationTime());
                    entry.setName(f.getName());
                    entry.setOwner("admin");
                    entry.setParentRef(parent.getUid());
                    register = registerDirLogicalData(entry, connection);
                }
                add((VDir) f, base, connection, ssid, addFiles);
            } else if (addFiles) {
                if (register == null) {
                    addFile(connection, (VFile) f, parent.getUid(), ssid);
                }
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

    public static void main(String args[]) {
        try {
            List<MyStorageSite> sites = new ArrayList<>();


            Credential credential = new Credential();
            credential.setStorageSiteUsername("fakeuser");
            credential.setStorageSitePassword("fakepass");


//            MyStorageSite ss1 = new MyStorageSite();
//            ss1.setCredential(credential);
//            ss1.setResourceURI("file:///tmp/");
//            ss1.setCurrentNum(Long.valueOf("-1"));
//            ss1.setCurrentSize(Long.valueOf("-1"));
//            ss1.setEncrypt(false);
//            ss1.setQuotaNum(Long.valueOf("-1"));
//            ss1.setQuotaSize(Long.valueOf("-1"));
//            sites.add(ss1);
//
//
//            MyStorageSite ss2 = new MyStorageSite();
//            ss2.setCredential(credential);
//            ss2.setResourceURI("file:///" + System.getProperty("user.home") + "/Downloads");
//            ss2.setCurrentNum(Long.valueOf("-1"));
//            ss2.setCurrentSize(Long.valueOf("-1"));
//            ss2.setEncrypt(false);
//            ss2.setQuotaNum(Long.valueOf("-1"));
//            ss2.setQuotaSize(Long.valueOf("-1"));
//            sites.add(ss2);


            MyStorageSite ss3 = new MyStorageSite();
            ss3.setCredential(credential);
            ss3.setResourceURI("file:///" + System.getProperty("user.home") + "/Downloads/files");
            ss3.setCurrentNum(Long.valueOf("-1"));
            ss3.setCurrentSize(Long.valueOf("-1"));
            ss3.setEncrypt(false);
            ss3.setQuotaNum(Long.valueOf("-1"));
            ss3.setQuotaSize(Long.valueOf("-1"));
            sites.add(ss3);

            Assimilator a = new Assimilator();
            a.assimilate(sites);


        } catch (ClassNotFoundException | SQLException | MalformedURLException | VlException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Assimilator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            VRS.exit();
        }

    }

    private void addFile(Connection connection, VFile f, Long parentRef, long ssID) throws SQLException, VlException, NoSuchAlgorithmException {
        long pdriGroupID = addPdrigroupTable(connection);
        LogicalData entry = new LogicalData();
        entry.setContentTypesAsString(f.getMimeType());
        entry.setCreateDate(f.getModificationTime());
        entry.setLength(f.getLength());
        entry.setModifiedDate(f.getModificationTime());
        entry.setName(f.getName());
        entry.setOwner("admin");

        entry.setParentRef(parentRef);
        entry.setType(Constants.LOGICAL_FILE);
        entry.setPdriGroupId(pdriGroupID);
//                        if(f instanceof VChecksum){
//                            String chs = ((VChecksum) f).getChecksum(VChecksum.MD5);
//                            entry.setChecksum();
//                        }
        long pdriID = addPDRI(connection, f.getName(), ssID, pdriGroupID, false, DesEncrypter.generateKey());
        addLogicalData(connection, entry);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRIDescr;

/**
 *
 * @author S. Koulouzis
 */
public class Assimilator {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/DB";
    //  Database credentials
    static final String USER = "user";
    static final String PASS = "pass";
    private final Connection conn;

    public Assimilator() throws ClassNotFoundException, SQLException {
        //STEP 2: Register JDBC driver
        Class.forName("com.mysql.jdbc.Driver");

        //STEP 3: Open a connection
        System.out.println("Connecting to database...");
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.setAutoCommit(false);
    }

    public static void main(String args[]) {
        try {
            Assimilator a = new Assimilator();
            Connection c = a.getConnection();

            long credentialsID = a.addCredentials(c, "faceuser", "facepass");
            String ssURI = "file:///tmp";

            long ssID = a.addStorageSite(c, ssURI, credentialsID, -1, -1, -1, -1, false, false);

            long pdriGroupID = a.addPdrigroupTable(c);

            String fileName = "test.out";
            long pdriID = a.addPDRI(c, fileName, ssID, pdriGroupID, false, DesEncrypter.generateKey());

            long parentRef = 1;
            String ownerId = "admin";
            String datatype = Constants.LOGICAL_FILE;
            long createDate = (System.currentTimeMillis());
            long modifiedDate = (System.currentTimeMillis());
            long ldLength = 300;
            String contentTypesStr = "text/plain";
            LogicalData entry = new LogicalData();
            entry.setContentTypesAsString(contentTypesStr);
            entry.setCreateDate(createDate);
            entry.setLength(ldLength);
            entry.setModifiedDate(modifiedDate);
            entry.setName(fileName);
            entry.setOwner(ownerId);
            entry.setParentRef(parentRef);
            entry.setType(datatype);
            entry.setPdriGroupId(pdriGroupID);
            a.addLogicalData(c, entry);
            c.commit();
        } catch (NoSuchAlgorithmException | ClassNotFoundException | SQLException ex) {
            Logger.getLogger(Assimilator.class.getName()).log(Level.SEVERE, null, ex);
        }

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

    private long addStorageSite(Connection connection, String storageSiteURI,
            long credentialRef, int currentNum, int currentSize, int quotaNum,
            int quotaSize, boolean isCache, boolean encrypt) throws SQLException {
        long ssID;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                        + "storage_site_table (resourceUri, credentialRef, "
                        + "currentNum, currentSize, quotaNum, quotaSize, "
                        + "isCache, encrypt) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, storageSiteURI);
            ps.setLong(2, credentialRef);
            ps.setInt(3, currentNum);
            ps.setInt(4, currentSize);
            ps.setInt(5, quotaNum);
            ps.setInt(6, quotaSize);
            ps.setBoolean(7, isCache);
            ps.setBoolean(8, encrypt);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ssID = rs.getLong(1);
//                connection.commit();
            System.out.println("ssID: " + ssID);
        }
//        }
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
        }
//        }
    }
}

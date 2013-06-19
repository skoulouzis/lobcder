package nl.uva.cs.lobcder.catalogue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.DesEncrypter;

/**
 * User: dvasunin Date: 25.02.13 Time: 17:28 To change this template use File |
 * Settings | File Templates.
 */
@Log
class ReplicateSweep implements Runnable {

    private final DataSource datasource;
    private boolean aggressiveReplicate = true;

    public ReplicateSweep(DataSource datasource) {
        this.datasource = datasource;
    }
    private Collection<StorageSite> availableStorage = null;
    private Iterator<StorageSite> it = null;

    private StorageSite findBestSite() {
        if (it == null || !it.hasNext()) {
            it = availableStorage.iterator();
        }
        return it.next();
    }

    private Collection<StorageSite> findBestSites() {
        if (aggressiveReplicate) {
            return availableStorage;
        } else {
            ArrayList<StorageSite> sites = new ArrayList<StorageSite>();
            if (it == null || !it.hasNext()) {
                it = availableStorage.iterator();
            }
            sites.add(it.next());
            return sites;
            //             
        }
    }

    private Collection<StorageSite> getStorageSites(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT storageSiteId, resourceURI, "
                    + "currentNum, currentSize, quotaNum, quotaSize, username, "
                    + "password, encrypt FROM storage_site_table JOIN credential_table ON "
                    + "credentialRef = credintialId WHERE isCache != TRUE");
            ArrayList<StorageSite> res = new ArrayList<>();
            while (rs.next()) {
                Credential c = new Credential();
                c.setStorageSiteUsername(rs.getString(7));
                c.setStorageSitePassword(rs.getString(8));
                StorageSite ss = new StorageSite();
                ss.setStorageSiteId(rs.getLong(1));
                ss.setCredential(c);
                ss.setResourceURI(rs.getString(2));
                ss.setCurrentNum(rs.getLong(3));
                ss.setCurrentSize(rs.getLong(4));
                ss.setQuotaNum(rs.getLong(5));
                ss.setQuotaSize(rs.getLong(6));
                ss.setEncrypt(rs.getBoolean(9));
                res.add(ss);
            }
            return res;
        }
    }

    class CacheDescr {

        Long pdriId;
        String name;
        Long pdriGroupRef;
    }

    private void onCacheReplicate(CacheDescr cd, CachePDRI cpdri, Connection connection) throws SQLException, IOException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pdri_table WHERE pdriId = ?")) {
            cpdri.delete();
            ps.setLong(1, cd.pdriId);
            ps.executeUpdate();
        }
    }

    private void onCacheReplicate(PDRIDescr cd, PDRI cpdri, Connection connection) throws SQLException, IOException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pdri_table WHERE pdriId = ?")) {
            cpdri.delete();
            ps.setLong(1, cd.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void run() {
        String name;
        long ssID;
        String resourceURL;
        String username;
        String password;
        boolean encrypt;
        BigInteger key;
        long keyLong;
        PDRI source;
        long pdriGroupRef;
        long pdriId;
        try (Connection connection = datasource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                availableStorage = getStorageSites(connection);
                ArrayList<PDRIDescr> toReplicate = new ArrayList<>();
                try (Statement statement = connection.createStatement()) {

//                    String sql = "SELECT pdriId, fileName, pdri_table.pdriGroupRef FROM pdri_table JOIN ("
//                            + "SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef "
//                            + "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId  "
//                            + "WHERE refcnt = 1 AND isCache LIMIT 100";
//                    ResultSet rs = statement.executeQuery(sql);
//                    while (rs.next()) {
//                        CacheDescr cd = new CacheDescr();
//                        cd.pdriId = rs.getLong(1);
//                        cd.name = rs.getString(2);
//                        cd.pdriGroupRef = rs.getLong(3);
//                        toReplicate.add(cd);
//                    }

                    String sql = "SELECT fileName, storageSiteId, storage_site_table.resourceUri, username, password, encrypt, encryptionKey, pdri_table.pdriGroupRef, "
                            + "pdri_table.pdriId "
                            + "FROM pdri_table JOIN (SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt "
                            + "FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef "
                            + "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId "
                            + "JOIN credential_table on credential_table.credintialId = storage_site_table.credentialRef "
                            + "WHERE refcnt = 1 AND isCache LIMIT 100";
                    ResultSet rs = statement.executeQuery(sql);
                    while (rs.next()) {
                        name = rs.getString(1);
                        ssID = rs.getLong(2);
                        resourceURL = rs.getString(3);
                        username = rs.getString(4);
                        password = rs.getString(5);
                        encrypt = rs.getBoolean(6);
                        keyLong = rs.getLong(7);
                        key = BigInteger.valueOf(keyLong);
                        pdriGroupRef = rs.getLong(8);
                        pdriId = rs.getLong(9);
                        PDRIDescr cd = new PDRIDescr(name, ssID, resourceURL, username, password, encrypt, key, pdriGroupRef, pdriId);
                        toReplicate.add(cd);
                    }

                }
                connection.commit();
                for (PDRIDescr cd : toReplicate) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table "
                                    + "(fileName, storageSiteRef, pdriGroupRef,isEncrypted, encryptionKey) VALUES(?, ?, ?, ?, ?)")) {
                        source = new PDRIFactory().createInstance(cd, false);
//                        StorageSite ss = findBestSite();
                        Collection<StorageSite> ss = findBestSites();
                        for (StorageSite s : ss) {
                            BigInteger pdriKey = DesEncrypter.generateKey();
                            PDRIDescr pdriDescr = new PDRIDescr(
                                    cd.getName(),
                                    s.getStorageSiteId(),
                                    s.getResourceURI(),
                                    s.getCredential().getStorageSiteUsername(),
                                    s.getCredential().getStorageSitePassword(), s.isEncrypt(), pdriKey, cd.getPdriGroupRef(), null);

                            PDRI replica = PDRIFactory.getFactory().createInstance(pdriDescr, false);
                            replica.replicate(source);
                            preparedStatement.setString(1, cd.getName());
                            preparedStatement.setLong(2, s.getStorageSiteId());
                            preparedStatement.setLong(3, cd.getPdriGroupRef());
                            preparedStatement.setBoolean(4, replica.getEncrypted());
                            preparedStatement.setLong(5, pdriKey.longValue());
                            preparedStatement.executeUpdate();
                        }
                        onCacheReplicate(cd, source, connection);
                        connection.commit();


                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(ReplicateSweep.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (SQLException | IOException e) {
                ReplicateSweep.log.log(Level.SEVERE, null, e);
                connection.rollback();
            }
        } catch (SQLException e) {
            ReplicateSweep.log.log(Level.SEVERE, null, e);
        }
    }
}

package nl.uva.cs.lobcder.catalogue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

/**
 * User: dvasunin Date: 25.02.13 Time: 17:28 To change this template use File |
 * Settings | File Templates.
 */
@Log
class ReplicateSweep implements Runnable {

    private final DataSource datasource;
    private static PropertiesHelper.ReplicationPolicy replicatePolicy = PropertiesHelper.ReplicationPolicy.firstSite;

    public ReplicateSweep(DataSource datasource) throws NamingException {
        this.datasource = datasource;
        try {
            replicatePolicy = PropertiesHelper.getReplicationPolicy();
        } catch (IOException ex) {
            Logger.getLogger(ReplicateSweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private Map<String, StorageSite> availableStorage = null;
    private Iterator<StorageSite> it = null;

    private Map<String, StorageSite> findBestSites(Connection connection) throws SQLException, IOException {
        switch (replicatePolicy) {
            case firstSite:
                Map<String, StorageSite> sites = new HashMap<>();
                if (it == null || !it.hasNext()) {
                    it = availableStorage.values().iterator();
                }
                StorageSite site = it.next();
                sites.put(site.getResourceURI(), site);
                return sites;
            case aggressive:
                return availableStorage;
            case fastest:
                return getFastestSites(connection, PropertiesHelper.getNumberOfSites());
            case random:
                return getRandomStorageSite(PropertiesHelper.getNumberOfSites());
            default:
                sites = new HashMap<>();
                if (it == null || !it.hasNext()) {
                    it = availableStorage.values().iterator();
                }
                site = it.next();
                sites.put(site.getResourceURI(), site);
                return sites;
        }
    }

    private Map<String, StorageSite> getStorageSites(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT storageSiteId, resourceURI, "
                    + "currentNum, currentSize, quotaNum, quotaSize, username, "
                    + "password, encrypt FROM storage_site_table JOIN credential_table ON "
                    + "credentialRef = credintialId WHERE isCache != TRUE");
            Map<String, StorageSite> res = new HashMap<>();
            while (rs.next()) {
                Credential c = new Credential();
                c.setStorageSiteUsername(rs.getString(7));
                c.setStorageSitePassword(rs.getString(8));
                StorageSite ss = new StorageSite();
                ss.setStorageSiteId(rs.getLong(1));
                ss.setCredential(c);
                String uri = rs.getString(2);
                ss.setResourceURI(uri);
                ss.setCurrentNum(rs.getLong(3));
                ss.setCurrentSize(rs.getLong(4));
                ss.setQuotaNum(rs.getLong(5));
                ss.setQuotaSize(rs.getLong(6));
                ss.setEncrypt(rs.getBoolean(9));
                res.put(uri, ss);
            }
            return res;
        }
    }

    private void replicate(PDRIDescr sourceDescr, PDRIDescr destinationDescr, PreparedStatement preparedStatement, BigInteger pdriKey, Connection connection) throws IOException, SQLException, NoSuchAlgorithmException {
        PDRI destinationPDRI = PDRIFactory.getFactory().createInstance(destinationDescr, false);
        PDRI sourcePDRI = new PDRIFactory().createInstance(sourceDescr, false);
        //                            destinationPDRI.setLength(length);
        if (!destinationPDRI.exists(sourcePDRI.getFileName())) {
            destinationPDRI.replicate(sourcePDRI);
        }
        try (Statement s2 = connection.createStatement()) {
            String name = sourceDescr.getName().replaceAll("'", "''");
            ResultSet rs = s2.executeQuery("select * from pdri_table WHERE "
                    + "(fileName LIKE '" + name + "'"
                    + " AND storageSiteRef = " + destinationPDRI.getStorageSiteId()
                    + " AND pdriGroupRef = " + destinationDescr.getPdriGroupRef()
                    + " AND isEncrypted = " + destinationPDRI.getEncrypted() + ")");
            if (!rs.next()) {
                preparedStatement.setString(1, sourceDescr.getName());
                preparedStatement.setLong(2, destinationPDRI.getStorageSiteId());
                preparedStatement.setLong(3, sourceDescr.getPdriGroupRef());
                preparedStatement.setBoolean(4, destinationPDRI.getEncrypted());
                preparedStatement.setLong(5, pdriKey.longValue());
                preparedStatement.executeUpdate();
            }
        }

    }

    private Map<String, StorageSite> getFastestSites(Connection connection, int number) throws SQLException {
        //Every now and then (20/80) go random
        int[] numsToGenerate = new int[]{1, 2};
        double[] discreteProbabilities = new double[]{0.85, 0.15};
        EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(numsToGenerate, discreteProbabilities);
        int random = distribution.sample();
        Map<String, StorageSite> res = new HashMap<>();
        if (random <= 1) {
            try (Statement s = connection.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT src, dst FROM speed_table "
                        + "GROUP BY averageSpeed DESC LIMIT " + number);
                Map<String, StorageSite> tmp = new HashMap<>();

                while (rs.next()) {
                    String src = rs.getString(1);
                    String dst = rs.getString(2);
                    Set<String> keys = availableStorage.keySet();
                    for (String k : keys) {
                        if (k.contains(src) || k.contains(dst)) {
                            tmp.put(k, availableStorage.get(k));
                        }
                    }
                }
                if (tmp.isEmpty()) {
                    ArrayList<String> keysAsArray = new ArrayList<>(availableStorage.keySet());
                    Random r = new Random();
                    StorageSite randomValue = availableStorage.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
                    res.put(randomValue.getResourceURI(), randomValue);
                } else {
                    ArrayList<String> keysAsArray = new ArrayList<>(tmp.keySet());
                    Random r = new Random();
                    StorageSite randomValue = tmp.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
                    res.put(randomValue.getResourceURI(), randomValue);
                }
            }
        } else {
            return getRandomStorageSite(number);
        }
        return res;


    }

    private Map<String, StorageSite> getRandomStorageSite(int number) {
        Map<String, StorageSite> res = new HashMap<>();
        List<StorageSite> copy = new LinkedList<>(availableStorage.values());
        Collections.shuffle(copy);
        if (number > availableStorage.size()) {
            number = availableStorage.size();
        }
        List<StorageSite> rand = copy.subList(0, number);
        for (StorageSite s : rand) {
            res.put(s.getResourceURI(), s);
        }
        return res;
    }

//    class CacheDescr {
//
//        Long pdriId;
//        String name;
//        Long pdriGroupRef;
//    }
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
        PDRI sourcePDRI;
        long pdriGroupRef;
        long pdriId;
        try (Connection connection = datasource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                if (availableStorage == null || availableStorage.isEmpty()) {
                    availableStorage = getStorageSites(connection);
                }

                ArrayList<PDRIDescr> toReplicate = new ArrayList<>();
                Map<Long, String> locationPerMap = new HashMap<>();
                try (Statement statement = connection.createStatement()) {

//                    String sql = "SELECT pdriId, fileName, pdri_table.pdriGroupRef FROM pdri_table JOIN ("
//                            + "SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef "
//                            + "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId  "
//                            + "WHERE refcnt = 1 AND isCache LIMIT 100";
//                    ResultSet rs = statement.executeQuery(sql);
//                    while (rs.next()) {
//                        CacheDescr sourceDescr = new CacheDescr();
//                        sourceDescr.pdriId = rs.getLong(1);
//                        sourceDescr.name = rs.getString(2);
//                        sourceDescr.pdriGroupRef = rs.getLong(3);
//                        toReplicate.add(sourceDescr);
//                    }

                    String sql = "SELECT fileName, storageSiteId, storage_site_table.resourceUri, username, password, encrypt, encryptionKey, pdri_table.pdriGroupRef, "
                            + "pdri_table.pdriId, ldata_table.locationPreference "
                            + "FROM pdri_table JOIN (SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt "
                            + "FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef "
                            + "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId "
                            + "JOIN credential_table on credential_table.credintialId = storage_site_table.credentialRef "
                            + " JOIN ldata_table on (ldata_table.pdriGroupRef =  pdri_table.pdriGroupRef AND ldata_table.lockTokenId is NULL)"
                            + "WHERE refcnt >= 1 AND isCache LIMIT 100";

                    /*
                     // Change to this SQL if we want to keep temporary files in the cache without staging them to the backend

                     String sql = "SELECT fileName, storageSiteId, storage_site_table.resourceUri, username, password, encrypt, encryptionKey, pdri_table.pdriGroupRef, "
                     + "pdri_table.pdriId, ldata_table.locationPreference "
                     + "FROM pdri_table JOIN (SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt "
                     + "FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef "
                     + "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId "
                     + "JOIN credential_table on credential_table.credintialId = storage_site_table.credentialRef "
                     + " JOIN ldata_table on (ldata_table.pdriGroupRef =  pdri_table.pdriGroupRef AND ldata_table.lockTokenId is NULL)"
                     + "WHERE refcnt >= 1 AND isCache AND ttlSec is NULL LIMIT 100";
                     */

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
                        locationPerMap.put(pdriGroupRef, rs.getString(10));
                        PDRIDescr cd = new PDRIDescr(name, ssID, resourceURL, username, password, encrypt, key, pdriGroupRef, pdriId);
                        toReplicate.add(cd);
                    }

                }
                connection.commit();
                for (PDRIDescr sourceDescr : toReplicate) {

                    log.log(Level.FINE, "to replicate: {0}", sourceDescr.getResourceUrl());
                    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table "
                            + "(fileName, storageSiteRef, pdriGroupRef,isEncrypted, encryptionKey) VALUES(?, ?, ?, ?, ?)")) {
                        sourcePDRI = new PDRIFactory().createInstance(sourceDescr, false);
                        //                        StorageSite ss = findBestSite();
                        Map<String, StorageSite> ss = findBestSites(connection);
                        boolean failed = false;
                        //Get the prefered store location for file
                        String loclPrefStr = locationPerMap.get(sourceDescr.getPdriGroupRef());
                        if (loclPrefStr != null && loclPrefStr.length() > 1) {
                            StorageSite destinationSite = availableStorage.get(loclPrefStr);
                            if (destinationSite != null) {
                                BigInteger pdriKey = DesEncrypter.generateKey();
                                PDRIDescr destinationDescr = new PDRIDescr(
                                        sourceDescr.getName(),
                                        destinationSite.getStorageSiteId(),
                                        destinationSite.getResourceURI(),
                                        destinationSite.getCredential().getStorageSiteUsername(),
                                        destinationSite.getCredential().getStorageSitePassword(), destinationSite.isEncrypt(), pdriKey, sourceDescr.getPdriGroupRef(), null);
                                try {
                                    replicate(sourceDescr, destinationDescr, preparedStatement, pdriKey, connection);
                                } catch (IOException ex) {
                                    failed = true;
                                    Logger.getLogger(ReplicateSweep.class.getName()).log(Level.WARNING, null, ex);
                                }
                                if (!failed) {
                                    onCacheReplicate(sourceDescr, sourcePDRI, connection);
                                }
                                connection.commit();

                                continue;
                            }
                        }

                        for (StorageSite destinationSite : ss.values()) {
                            BigInteger pdriKey = DesEncrypter.generateKey();
                            PDRIDescr destinationDescr = new PDRIDescr(
                                    sourceDescr.getName(),
                                    destinationSite.getStorageSiteId(),
                                    destinationSite.getResourceURI(),
                                    destinationSite.getCredential().getStorageSiteUsername(),
                                    destinationSite.getCredential().getStorageSitePassword(), destinationSite.isEncrypt(), pdriKey, sourceDescr.getPdriGroupRef(), null);

                            try {
                                replicate(sourceDescr, destinationDescr, preparedStatement, pdriKey, connection);
                            } catch (IOException ex) {
                                //Add Sleep here 
                                failed = true;
                                Logger.getLogger(ReplicateSweep.class.getName()).log(Level.WARNING, null, ex);
                                continue;
                            }
                        }
                        if (!failed) {
                            onCacheReplicate(sourceDescr, sourcePDRI, connection);
                        }
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

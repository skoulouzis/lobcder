package nl.uva.cs.lobcder.catalogue;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.DesEncrypter;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by dvasunin on 14.01.15.
 */

@Log
public class ReplicateSweep1 implements Runnable {
    private final DataSource datasource;


    public ReplicateSweep1(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        boolean successFlag = true;
        try (Connection connection = datasource.getConnection())
        {
            connection.setAutoCommit(true);
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "UPDATE pdrigroup_table SET needCheck=FALSE WHERE pdriGroupId=?"))
            {
                for (Long pdriGroup : selectPdriGroupsToRelocate(connection)) {
                    Set<Long> preferences = new HashSet<>();
                    for (Long logicalDataId : getFilesByPdriGroup(pdriGroup, connection)) {
                        preferences.addAll(getPreferencesForFile(logicalDataId, connection));
                    }
                    if (!preferences.isEmpty()) {
                        for (PDRIDescr pdriDescr : getPdriDescrForGroup(pdriGroup, connection)) {
                            preferences.remove(pdriDescr.getStorageSiteId());
                        }
                        successFlag = replicate(pdriGroup, preferences, connection);
                    }
                    Collection<Long> removingStorage = getRemovingStorage(connection);
                    Collection<PDRIDescr> wantRemove = new ArrayList<>();
                    Collection<PDRIDescr> pdriDescrs = getPdriDescrForGroup(pdriGroup, connection);
                    Iterator<PDRIDescr> pdriDescrIt = pdriDescrs.iterator();
                    while(pdriDescrIt.hasNext()){
                        PDRIDescr pdriDescr = pdriDescrIt.next();
                        if(removingStorage.contains(pdriDescr.getStorageSiteId())){
                            wantRemove.add(pdriDescr);
                            pdriDescrIt.remove();
                        }
                    }
                    if(pdriDescrs.isEmpty()){
                        successFlag &= replicate(pdriGroup, getReplicationPolicy().getSitesToReplicate(connection), connection);
                    }
                    if(successFlag) {
                        successFlag = removePdris(wantRemove, connection);
                    }
                    if(successFlag) {
                        successFlag = removeCache(pdriGroup, connection);
                    }
                    if(successFlag){
                        preparedStatement.setLong(1, pdriGroup);
                        preparedStatement.executeUpdate();
                    }
                }
            }
        } catch(Exception e) {
            ReplicateSweep1.log.log(Level.SEVERE, null, e);
        }
    }

    private boolean removeCache(Long pdriGroup, Connection connection) {
        boolean result = true;
        try (PreparedStatement preparedStatementSelect = connection.prepareStatement("SELECT fileName, storageSiteRef, storage_site_table.resourceUri, username, password, isEncrypted, encryptionKey, pdri_table.pdriId FROM pdri_table JOIN storage_site_table ON storageSiteRef = storageSiteId JOIN credential_table ON credentialRef = credintialId WHERE pdri_table.pdriGroupRef=? AND isCache=TRUE" );
             PreparedStatement preparedStatementDel = connection.prepareStatement("DELETE FROM pdri_table WHERE pdriId=?")) {
            preparedStatementSelect.setLong(1, pdriGroup);
            Collection<PDRIDescr> cachePdris = new ArrayList<>();
            ResultSet rs = preparedStatementSelect.executeQuery();
            while (rs.next()) {
                String fileName = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                boolean encrypt = rs.getBoolean(6);
                long key = rs.getLong(7);
                long pdriId = rs.getLong(8);
                cachePdris.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), pdriGroup, pdriId));
            }
            for(PDRIDescr pdriDescr : cachePdris) {
                try{
                    PDRI pdri = PDRIFactory.getFactory().createInstance(pdriDescr);
                    pdri.delete();
                    log.log(Level.FINE, "PDRI Instance file name: {0}", new Object[]{pdri.getFileName()});
                    preparedStatementDel.setLong(1, pdriDescr.getId());
                    preparedStatementDel.executeUpdate();
                    log.log(Level.FINE, "DELETE:", pdri.getURI());
                } catch (Exception e) {
                    log.log(Level.WARNING, null, e);
                    result = false;
                }
            }
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, null, e);
            return false;
        }
    }

    private boolean removePdris(Collection<PDRIDescr> wantRemove, Connection connection) {
        if (wantRemove.isEmpty())
            return true;
        boolean result = true;
        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM pdri_table WHERE pdriId=?")) {
            for (PDRIDescr pdriDescr : wantRemove) {
                try {
                    PDRI pdri = PDRIFactory.getFactory().createInstance(pdriDescr);
                    log.log(Level.FINE, "PDRI Instance file name: {0}", new Object[]{pdri.getFileName()});
                    pdri.delete();
                    preparedStatement.setLong(1, pdriDescr.getId());
                    preparedStatement.executeUpdate();
                    log.log(Level.FINE, "DELETE:", pdri.getURI());
                } catch (Exception e) {
                    log.log(Level.SEVERE, null, e);
                    result = false;
                }
            }
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, null, e);
            return false;
        }
    }

    private Collection<Long> getRemovingStorage(Connection connection) throws SQLException {
        try(Statement statement = connection.createStatement()) {
            Collection<Long> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery("SELECT storageSiteId FROM storage_site_table WHERE removing=TRUE");
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }

    private Collection<Long> getPreferencesForFile(Long uid, Connection connection) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("SELECT storageSiteRef FROM pref_table WHERE ld_uid=?")) {
            Collection<Long> result = new ArrayList<>();
            preparedStatement.setLong(1, uid);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }

    private Collection<Long> getFilesByPdriGroup(Long pdriGroup, Connection connection) throws SQLException {
        try(PreparedStatement preparedStatement = connection.prepareStatement("SELECT uid FROM ldata_table WHERE pdriGroupRef=?")) {
            Collection<Long> result = new ArrayList<>();
            preparedStatement.setLong(1, pdriGroup);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }

    private ReplicationPolicy getReplicationPolicy() {
        return new RandomReplicationPolicy();
    }

    private Collection<Long> selectPdriGroupsToRelocate(Connection connection) throws SQLException {
        try(Statement statement = connection.createStatement()) {
            Collection<Long> result = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery("SELECT pdriGroupId FROM pdrigroup_table WHERE needCheck=TRUE AND bound=FALSE LIMIT 10");
            while (resultSet.next()) {
                result.add(resultSet.getLong(1));
            }
            return result;
        }
    }

    private Collection<PDRIDescr> getPdriDescrForGroup(Long groupId, Connection connection) throws SQLException {
        Collection<PDRIDescr> res = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT fileName, storageSiteRef, storage_site_table.resourceUri, username, password, isEncrypted, encryptionKey, pdri_table.pdriId FROM pdri_table JOIN storage_site_table ON storageSiteRef = storageSiteId JOIN credential_table ON credentialRef = credintialId WHERE pdri_table.pdriGroupRef=? AND isCache=FALSE" ))
        {
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
                long pdriId = rs.getLong(8);
                res.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), groupId, pdriId));
            }
            return res;
        }
    }

    private PDRIDescr getSourcePdriDescrForGroup(Long groupId, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT fileName, storageSiteRef, storage_site_table.resourceUri, username, password, isEncrypted, encryptionKey, pdri_table.pdriId FROM pdri_table JOIN storage_site_table ON storageSiteRef = storageSiteId JOIN credential_table ON credentialRef = credintialId WHERE pdri_table.pdriGroupRef=? AND isCache=TRUE LIMIT 1" ))
        {
            ps.setLong(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String fileName = rs.getString(1);
                long ssID = rs.getLong(2);
                String resourceURI = rs.getString(3);
                String uName = rs.getString(4);
                String passwd = rs.getString(5);
                boolean encrypt = rs.getBoolean(6);
                long key = rs.getLong(7);
                long pdriId = rs.getLong(8);
                return new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), groupId, pdriId);
            } else {     // for optimization we better to use a smarter algorithm to pick up better source
                Collection<PDRIDescr> var = getPdriDescrForGroup(groupId, connection);
                PDRIDescr[] pdriDescrs = var.toArray(new PDRIDescr[var.size()]);
                return pdriDescrs[new Random().nextInt(pdriDescrs.length)];
            }
        }
    }

    private StorageSite getStorageSiteById(Long storageSiteId, Connection connection) throws SQLException {
        try (PreparedStatement s = connection.prepareStatement(
                "SELECT storageSiteId, resourceURI, currentNum, currentSize, quotaNum, quotaSize, username, password, encrypt, isCache FROM storage_site_table JOIN credential_table ON credentialRef = credintialId WHERE storageSiteId=?")) {
            StorageSite ss = null;
            s.setLong(1, storageSiteId);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                ss = new StorageSite();
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
                ss.setCache(rs.getBoolean(10));
            }
            return ss;
        }
    }

    private String generateFileName(PDRIDescr pdriDescr){
        return pdriDescr.getName();
    }

    private boolean replicate(Long pdriGroupId, Collection<Long> toReplicate, Connection connection) {
        if (toReplicate.isEmpty())
            return true;
        boolean result = true;
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO pdri_table (fileName, storageSiteRef, pdriGroupRef, isEncrypted, encryptionKey) VALUES (?,?,?,?,?)")) {
            PDRIDescr sourceDescr = getSourcePdriDescrForGroup(pdriGroupId, connection);
            PDRI sourcePdri = PDRIFactory.getFactory().createInstance(sourceDescr);

            for (Long site : toReplicate) {
                try {
                    StorageSite ss = getStorageSiteById(site, connection);
                    BigInteger pdriKey = DesEncrypter.generateKey();
                    PDRIDescr destinationDescr = new PDRIDescr(
                            generateFileName(sourceDescr),
                            ss.getStorageSiteId(),
                            ss.getResourceURI(),
                            ss.getCredential().getStorageSiteUsername(),
                            ss.getCredential().getStorageSitePassword(),
                            ss.isEncrypt(),
                            pdriKey,
                            pdriGroupId,
                            null);
                    PDRI destinationPdri = PDRIFactory.getFactory().createInstance(destinationDescr);
                    if (!destinationPdri.exists(destinationPdri.getFileName())) {
                        destinationPdri.replicate(sourcePdri);
                    }
                    preparedStatement.setString(1, destinationDescr.getName());
                    preparedStatement.setLong(2, destinationDescr.getStorageSiteId());
                    preparedStatement.setLong(3, destinationDescr.getPdriGroupRef());
                    preparedStatement.setBoolean(4, destinationDescr.getEncrypt());
                    preparedStatement.setLong(5, destinationDescr.getKey().longValue());
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    log.log(Level.WARNING, null, e);
                    result = false;
                }
            }
            return result;
        } catch (Exception e) {
            log.log(Level.SEVERE, null, e);
            return false;
        }
    }

}

/*


SELECT
  ldata_table.pdriGroupRef,
  pref_table.storageSiteRef
FROM pref_table
  JOIN ldata_table
    ON pref_table.ld_uid = ldata_table.uid
WHERE ldata_table.datatype = 'logical.file' AND pref_table.storageSiteRef NOT IN (
  SELECT storageSiteRef
  FROM pdri_table
  WHERE pdri_table.pdriGroupRef = ldata_table.pdriGroupRef
)
UNION
SELECT DISTINCT
  pdri_table.pdriGroupRef,
  NULL
FROM pdri_table
  LEFT JOIN (SELECT
               pdri_table.pdriGroupRef,
               count(pdri_table.storageSiteRef) AS refcnt
             FROM pdri_table
               JOIN storage_site_table
                 ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
             WHERE NOT (storage_site_table.isCache OR storage_site_table.isRemoving)
             GROUP BY pdriGroupRef) AS t
    ON pdri_table.pdriGroupRef = t.pdriGroupRef
WHERE t.refcnt IS NULL
LIMIT 100;

select pdri_table.pdriId
from pdri_table
join (
SELECT
  pdri_table.pdriGroupRef,
  count(pdri_table.storageSiteRef) - IFNULL(refcnt1, 0)  as diff
FROM pdri_table
  JOIN storage_site_table
    ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
  LEFT JOIN (
    SELECT
      pdri_table.pdriGroupRef,
      count(pdri_table.storageSiteRef) AS refcnt1
    FROM pdri_table
      JOIN storage_site_table
        ON pdri_table.storageSiteRef = storage_site_table.storageSiteId
    WHERE storage_site_table.isCache OR storage_site_table.isRemoving
    GROUP BY pdriGroupRef
    ) as t
  on pdri_table.pdriGroupRef = t.pdriGroupRef
GROUP BY pdriGroupRef
HAVING diff>0) as t2
ON pdri_table.pdriGroupRef=t2.pdriGroupRef
JOIN storage_site_table
ON pdri_table.storageSiteRef=storage_site_table.storageSiteId
where storage_site_table.isCache or storage_site_table.isRemoving;

SELECT
  pdri_table.pdriId
FROM pdri_table
JOIN (
    SELECT pdrigroup_table.pdriGroupId
    FROM pdrigroup_table
    WHERE pdriGroupId NOT IN (
      SELECT DISTINCT ldata_table.pdriGroupRef
      FROM ldata_table
        LEFT JOIN pref_table
          ON ldata_table.uid = pref_table.ld_uid
      WHERE pref_table.ld_uid IS NULL
    ) ) as t1
ON pdri_table.pdriGroupRef = t1.pdriGroupId
WHERE pdri_table.storageSiteRef not in (
  SELECT
    pref_table.storageSiteRef
  FROM pref_table
    JOIN ldata_table
    ON ldata_table.uid = pref_table.ld_uid
  where ldata_table.pdriGroupRef = t1.pdriGroupId);

 */
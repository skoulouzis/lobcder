package nl.uva.cs.lobcder.catalogue;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * User: dvasunin
 * Date: 25.02.13
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 */

@Log
class ReplicateSweep implements Runnable {

    private final DataSource datasource;

    public ReplicateSweep(DataSource datasource) {
        this.datasource = datasource;
    }

    private Collection<MyStorageSite> availableStorage = null;

    private Iterator<MyStorageSite> it = null;

    private MyStorageSite findBestSite() {
        if (it == null || !it.hasNext()) {
            it = availableStorage.iterator();
        }
        return it.next();
    }


    private Collection<MyStorageSite> getStorageSites(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()){
            ResultSet rs = s.executeQuery("SELECT storageSiteId, resourceURI, currentNum, currentSize, quotaNum, quotaSize, username, password FROM storage_site_table JOIN credential_table ON credentialRef = credintialId WHERE isCache != TRUE");
            ArrayList<MyStorageSite> res = new ArrayList<>();
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
            return res;
        }
    }

    class CacheDescr{
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

    @Override
    public void run() {
        try (Connection connection = datasource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                availableStorage = getStorageSites(connection);
                ArrayList<CacheDescr> toReplicate = new ArrayList<>();
                try (Statement statement = connection.createStatement()) {
                    String sql = "SELECT pdriId, fileName, pdri_table.pdriGroupRef FROM pdri_table JOIN (" +
                            "SELECT  pdriGroupRef, count(pdri_table.storageSiteRef) AS refcnt FROM pdri_table GROUP BY pdriGroupRef)  AS t ON pdri_table.pdriGroupRef = t.pdriGroupRef " +
                            "JOIN storage_site_table ON pdri_table.storageSiteRef = storage_site_table.storageSiteId  " +
                            "WHERE refcnt = 1 AND isCache LIMIT 100";
                    ResultSet rs = statement.executeQuery(sql);
                    while (rs.next()) {
                        CacheDescr cd = new CacheDescr();
                        cd.pdriId = rs.getLong(1);
                        cd.name = rs.getString(2);
                        cd.pdriGroupRef = rs.getLong(3);
                        toReplicate.add(cd);
                    }
                }
                connection.commit();
                for (CacheDescr cd : toReplicate) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO pdri_table (fileName, storageSiteRef, pdriGroupRef) VALUES(?, ?, ?)")) {
                        CachePDRI source = new CachePDRI(cd.name);
                        MyStorageSite ss = findBestSite();
                        PDRIDescr pdriDescr = new PDRIDescr(
                                cd.name,
                                ss.getStorageSiteId(),
                                ss.getResourceURI(),
                                ss.getCredential().getStorageSiteUsername(),
                                ss.getCredential().getStorageSitePassword()
                        );
                        PDRI replica = PDRIFactory.getFactory().createInstance(pdriDescr);
                        replica.replicate(source);
                        preparedStatement.setString(1, cd.name);
                        preparedStatement.setLong(2, ss.getStorageSiteId());
                        preparedStatement.setLong(3, cd.pdriGroupRef);
                        preparedStatement.executeUpdate();
                        onCacheReplicate(cd, source, connection);
                        connection.commit();
                    }
                }
            } catch (Exception e) {
                ReplicateSweep.log.log(Level.SEVERE, null, e);
                connection.rollback();
            }
        } catch (SQLException e) {
            ReplicateSweep.log.log(Level.SEVERE, null, e);
        }
    }
}

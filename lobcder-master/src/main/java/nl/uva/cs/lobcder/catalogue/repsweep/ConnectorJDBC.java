package nl.uva.cs.lobcder.catalogue.repsweep;

import nl.uva.cs.lobcder.catalogue.beans.*;
import nl.uva.cs.lobcder.resources.PDRIDescr;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dvasunin on 28.02.15.
 */
public class ConnectorJDBC implements ConnectorI {


    private final DataSource datasource;

    private final Integer limit;

    public ConnectorJDBC(DataSource datasource, Integer limit) {
        this.datasource = datasource;
        this.limit = limit;
    }


//     mysql> SELECT DISTINCT pdriGroupId, now() FROM pdrigroup_table WHERE bound=FALSE AND refCount>0 AND (exists(select * from pdri_table where pdri_table.pdriGroupRef=pdrigroup_table.pdriGroupId AND pdri_table.storageSiteRef=3) OR NOT exists(select * from pdri_table join storage_site_table on pdri_table.storageSiteRef=storage_site_table.storageSiteId where pdri_table.pdriGroupRef=pdrigroup_table.pdriGroupId AND storage_site_table.isCache=true)) limit 20;

    @Override
    public Set<PdriGroupBean> selectPdriGroupsToRelocate(Long localCacheId) throws Exception {
        Set<PdriGroupBean> pdriGroups = new HashSet<>();
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (CallableStatement callableStatement = connection.prepareCall("CALL GET_PDRI_GROUPS_FOR_REPLICATE(?,?)");
                 PreparedStatement preparedStatementPdri = connection.prepareStatement(
                         "SELECT pdri_table.pdriId, pdri_table.fileName, pdri_table.isEncrypted, pdri_table.encryptionKey, "
                                 + "storageSiteRef, storage_site_table.resourceUri, storage_site_table.encrypt, storage_site_table.isCache, storage_site_table.extra, "
                                 + "credential_table.username, credential_table.password "
                                 + "FROM pdri_table JOIN storage_site_table "
                                 + "ON storageSiteRef = storageSiteId "
                                 + "JOIN credential_table "
                                 + "ON credentialRef = credintialId "
                                 + "WHERE pdri_table.pdriGroupRef=?"

                 );
                 PreparedStatement preparedStatementItem = connection.prepareStatement("SELECT uid FROM ldata_table WHERE pdriGroupRef=?");
                 PreparedStatement preparedStatementPref = connection.prepareStatement(
                         "SELECT storageSiteRef, resourceUri, encrypt, isCache, extra, username, password "
                                 + "FROM pref_table JOIN storage_site_table "
                                 + "ON storageSiteRef = storageSiteId "
                                 + "JOIN credential_table "
                                 + "ON  credentialRef = credintialId "
                                 + "WHERE ld_uid=?")
            ) {
                callableStatement.setLong(1, localCacheId);
                callableStatement.setInt(2, limit);
                ResultSet rs1 = callableStatement.executeQuery();
                while (rs1.next()) {
                    PdriGroupBean pdriGroupBean = new PdriGroupBean();
                    pdriGroupBean.setId(rs1.getLong(1));
                    pdriGroupBean.setBound(Boolean.FALSE);
                    pdriGroupBean.setNeedCheck(Boolean.TRUE);
                    pdriGroups.add(pdriGroupBean);

                    preparedStatementPdri.setLong(1, pdriGroupBean.getId());
                    ResultSet rsPdri = preparedStatementPdri.executeQuery();
                    if (rsPdri.next()) {
                        pdriGroupBean.setPdri(new HashSet<PdriBean>());
                        do {
                            pdriGroupBean.getPdri().add(
                                    new PdriBean(
                                            rsPdri.getLong(1),
                                            rsPdri.getString(2),
                                            rsPdri.getBoolean(3) ? BigInteger.valueOf(rsPdri.getLong(4)) : null,
                                            new StorageSiteBean(
                                                    rsPdri.getLong(5),
                                                    rsPdri.getString(6),
                                                    rsPdri.getBoolean(7),
                                                    rsPdri.getBoolean(8),
                                                    rsPdri.getString(9),
                                                    new CredentialBean(
                                                            rsPdri.getString(10),
                                                            rsPdri.getString(11)
                                                    )
                                            )
                                    )
                            );
                        } while (rsPdri.next());
                    }

                    preparedStatementItem.setLong(1, pdriGroupBean.getId());
                    ResultSet rsItem = preparedStatementItem.executeQuery();
                    if (rsItem.next()) {
                        pdriGroupBean.setItem(new HashSet<ItemBean>());
                        do {
                            ItemBean itemBean = new ItemBean();
                            itemBean.setUid(rsItem.getLong(1));
                            pdriGroupBean.getItem().add(itemBean);

                            preparedStatementPref.setLong(1, itemBean.getUid());
                            ResultSet rsPref = preparedStatementPref.executeQuery();
                            if (rsPref.next()) {
                                itemBean.setPreference(new HashSet<StorageSiteBean>());
                                do {
                                    itemBean.getPreference().add(
                                            new StorageSiteBean(
                                                    rsPdri.getLong(1),
                                                    rsPdri.getString(2),
                                                    rsPdri.getBoolean(3),
                                                    rsPdri.getBoolean(4),
                                                    rsPdri.getString(5),
                                                    new CredentialBean(
                                                            rsPdri.getString(6),
                                                            rsPdri.getString(7)
                                                    )
                                            )
                                    );
                                } while (rsPref.next());
                            }
                        } while (rsItem.next());
                    }
                }
            }
            return pdriGroups;
        }
    }

    @Override
    public Set<StorageSiteBean> getRemovingStorage() throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                Set<StorageSiteBean> result = new HashSet<>();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT storageSiteId, resourceUri, encrypt, isCache, extra, username, password "
                                + "FROM storage_site_table JOIN credential_table "
                                + "ON  credentialRef = credintialId "
                                + "WHERE removing=TRUE");
                while (resultSet.next()) {
                    result.add(
                            new StorageSiteBean(
                                    resultSet.getLong(1),
                                    resultSet.getString(2),
                                    resultSet.getBoolean(3),
                                    resultSet.getBoolean(4),
                                    resultSet.getString(5),
                                    new CredentialBean(
                                            resultSet.getString(6),
                                            resultSet.getString(7)
                                    )
                            )
                    );
                }
                return result;
            }
        }
    }

    @Override
    public void reportNewReplica(PdriBean pdriBean, PdriGroupBean pdriGroupBean) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try(PreparedStatement ps = connection.prepareStatement("INSERT INTO pdri_table(fileName,storageSiteRef,pdriGroupRef,isEncrypted,encryptionKey) VALUES(?,?,?,?,?)")){
                ps.setString(1,pdriBean.getName());
                ps.setLong(2, pdriBean.getStorage().getId());
                ps.setLong(3, pdriGroupBean.getId());
                ps.setBoolean(4, pdriBean.getStorage().getEncrypt());
                ps.setLong(5, pdriBean.getEncryptionKey().longValue());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void reportDeletedPdri(PdriBean pdriBean) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pdri_table WHERE pdriId=?")) {
                ps.setLong(1, pdriBean.getId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void reportPdriGroupDone(PdriGroupBean pdriGroupBean) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM replicate_table WHERE pdriGroupRef=?");
                 PreparedStatement ps2 = connection.prepareStatement("UPDATE pdrigroup_table SET needCheck=FALSE WHERE pdriGroupId=?")
            ) {
                ps1.setLong(1, pdriGroupBean.getId());
                ps1.executeUpdate();
                ps2.setLong(1, pdriGroupBean.getId());
                ps2.executeUpdate();
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    @Override
    public void reportPdriGroupRelease(PdriGroupBean pdriGroupBean) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM replicate_table WHERE pdriGroupRef=?")) {
                ps.setLong(1, pdriGroupBean.getId());
                ps.executeUpdate();
            }
        }
    }

}

package nl.uva.cs.lobcder.catalogue.delsweep;

import nl.uva.cs.lobcder.catalogue.beans.CredentialBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by dvasunin on 26.02.15.
 */
public class ConnectorJDBC implements ConnectorI {

    private final DataSource datasource;

    private final Integer limit;

    public ConnectorJDBC(DataSource datasource, Integer limit) {
        this.datasource = datasource;
        this.limit = limit;
    }

    @Override
    public Collection<PdriGroupBean> getPdriGroupsToProcess() throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            ArrayList<Long> pdriGroups = new ArrayList<>();
            try (Statement s1 = connection.createStatement()) {
                int retval[] = {1};
                s1.executeUpdate("INSERT INTO delete_table (pdriGroupRef, selTimestamp) SELECT pdriGroupId, now() FROM pdrigroup_table LEFT OUTER JOIN delete_table ON pdrigroup_table.pdriGroupId = delete_table.pdriGroupRef WHERE delete_table.pdriGroupRef IS NULL AND pdrigroup_table.refCount = 0 LIMIT " + limit, retval);
                ResultSet rs = s1.getGeneratedKeys();
                while (rs.next()) {
                    pdriGroups.add(rs.getLong(1));
                }
            }
            ArrayList<PdriGroupBean> result = new ArrayList<>(pdriGroups.size());
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT pdriId, fileName, storageSiteRef, resourceUri, username, password FROM pdri_table JOIN storage_site_table ON storage_site_table.storageSiteId = pdri_table.storageSiteRef JOIN credential_table ON credentialRef = credintialId WHERE pdriGroupRef = ?")) {
                for (Long pdrigrId : pdriGroups) {
                    ps.setLong(1, pdrigrId);
                    ResultSet rs = ps.executeQuery();
                    PdriGroupBean pdriGroupBean = new PdriGroupBean();
                    pdriGroupBean.setId(pdrigrId);
                    if (rs.next()) {
                        ArrayList<PdriBean> pdriBeans = new ArrayList<>();
                        do {
                            pdriBeans.add(
                                    new PdriBean(
                                            rs.getLong(1),
                                            rs.getString(2),
                                            null,
                                            new StorageSiteBean(
                                                    rs.getLong(3),
                                                    rs.getString(4),
                                                    null, null, null,
                                                    new CredentialBean(rs.getString(5), rs.getString(6))
                                            )
                                    )
                            );
                        } while (rs.next());
                        pdriGroupBean.setPdri(pdriBeans);
                    }
                    result.add(pdriGroupBean);
                }
            }
            return result;
        }
    }

    @Override
    public void confirmPdriGroup(Long pdriGroupId) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pdrigroup_table WHERE pdriGroupId=?")) {
                ps.setLong(1,pdriGroupId);
                ps.executeUpdate();
            }
        }
    }
}

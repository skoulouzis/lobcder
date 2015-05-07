package nl.uva.cs.lobcder.catalogue.delsweep;

import nl.uva.cs.lobcder.catalogue.beans.CredentialBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.catalogue.beans.PdriGroupBean;
import nl.uva.cs.lobcder.catalogue.beans.StorageSiteBean;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by dvasunin on 26.02.15.
 */
public class ConnectorJDBC implements ConnectorI {

    private final DataSource datasource;
    private final Integer limit;
    private int reconnectAttemts = 0;
    private int sleeTime = 100;

    public ConnectorJDBC(DataSource datasource, Integer limit) {
        this.datasource = datasource;
        this.limit = limit;
    }

    @Override
    public Collection<PdriGroupBean> getPdriGroupsToProcess() throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            ArrayList<Long> pdriGroups = new ArrayList<>();
            try (CallableStatement s1 = connection.prepareCall("call GET_PDRI_GROUPS_FOR_DELETE(?)")) {
                s1.setInt(1, limit);
                ResultSet rs = s1.executeQuery();
                while (rs.next()) {
                    pdriGroups.add(rs.getLong(1));
                }
            }
            ArrayList<PdriGroupBean> result = new ArrayList<>(pdriGroups.size());
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT pdriId, fileName, storageSiteRef, resourceUri, username, password "
                    + "FROM pdri_table "
                    + "JOIN storage_site_table ON storage_site_table.storageSiteId = pdri_table.storageSiteRef "
                    + "JOIN credential_table ON storage_site_table.credentialRef = credintialId "
                    + "WHERE pdriGroupRef = ?")) {
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
                                    new CredentialBean(rs.getString(5), rs.getString(6)))));
                        } while (rs.next());
                        pdriGroupBean.setPdri(pdriBeans);
                    }
                    result.add(pdriGroupBean);
                }
            }
            reconnectAttemts = 0;
            sleeTime = 100;
            return result;
        }
//        catch (Exception ex) {
//            reconnectAttemts++;
//            if (reconnectAttemts < Constants.RECONNECT_NTRY) {
//                sleeTime = sleeTime * 2;
//                Thread.sleep(sleeTime);
//                return getPdriGroupsToProcess();
//            } else {
//                reconnectAttemts = 0;
//                sleeTime = 100;
//                throw ex;
//            }
//        }
    }

    @Override
    public void confirmPdriGroup(Long pdriGroupId) throws Exception {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM pdrigroup_table WHERE pdriGroupId=?")) {
                ps.setLong(1, pdriGroupId);
                ps.executeUpdate();
            }
            reconnectAttemts = 0;
            sleeTime = 50;
        }
//        catch (Exception ex) {
//            reconnectAttemts++;
//            if (reconnectAttemts < Constants.RECONNECT_NTRY) {
//                sleeTime = sleeTime * 2;
//                Thread.sleep(sleeTime);
//                confirmPdriGroup(pdriGroupId);
//            } else {
//                reconnectAttemts = 0;
//                throw ex;
//            }
//        }
    }
}

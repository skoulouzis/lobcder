package nl.uva.cs.lobcder.catalogue;

import java.io.IOException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Level;

/**
 * User: dvasunin
 * Date: 25.02.13
 * Time: 16:31
 * To change this template use File | Settings | File Templates.
 */
@Log
class DeleteSweep implements Runnable {

    private final DataSource datasource;

    public DeleteSweep(DataSource datasource) {
        this.datasource = datasource;
    }

//    @Override
//    public void run() {
//        try (Connection connection = datasource.getConnection()) {
//            try {
//                connection.setAutoCommit(false);
//                try (Statement s1 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
//                        java.sql.ResultSet.CONCUR_UPDATABLE)) {
//                    ResultSet rs1 = s1.executeQuery("SELECT pdriGroupId FROM pdrigroup_table WHERE refCount = 0");
//                    while (rs1.next()) {
//                        Long groupId = rs1.getLong(1);
//                        try (PreparedStatement ps2 = connection.prepareStatement(
//                                "SELECT fileName, storageSiteRef, resourceUri, username, password FROM pdri_table "
//                                        + "JOIN storage_site_table ON storageSiteRef = storageSiteId "
//                                        + "JOIN credential_table ON credentialRef = credintialId "
//                                        + "WHERE pdriGroupRef = ?",
//                                java.sql.ResultSet.TYPE_FORWARD_ONLY,
//                                java.sql.ResultSet.CONCUR_UPDATABLE)) {
//                            ps2.setLong(1, groupId);
//                            ResultSet rs2 = ps2.executeQuery();
//                            while (rs2.next()) {
//                                PDRIDescr pdriDescr = new PDRIDescr(
//                                        rs2.getString(1),
//                                        rs2.getLong(2),
//                                        rs2.getString(3),
//                                        rs2.getString(4),
//                                        rs2.getString(5));
//
//                                PDRI pdri = PDRIFactory.getFactory().createInstance(pdriDescr);
//                                pdri.delete();
//                                DeleteSweep.log.log(Level.FINE, "DELETE: {0}", pdri.getURI());
//                                rs2.deleteRow();
//                                connection.commit();
//                            }
//                            rs1.deleteRow();
//                            connection.commit();
//                        }
//                    }
//                }
//            } catch (SQLException | IOException e) {
//                DeleteSweep.log.log(Level.SEVERE, null, e);
//                connection.rollback();
//            }
//        } catch (SQLException e) {
//            DeleteSweep.log.log(Level.SEVERE, null, e);
//        }
//    }
    @Override
    public void run() {
        Connection connection = null;
        Statement s1 = null;
        Statement s2 = null;
        try {
            connection = datasource.getConnection();
            connection.setAutoCommit(false);
            s1 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            s2 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_UPDATABLE);
            ResultSet rs1 = s1.executeQuery("SELECT pdriGroupId FROM pdrigroup_table WHERE refCount = 0");
            while (rs1.next()) {
                Long groupId = rs1.getLong(1);
                ResultSet rs2 = s2.executeQuery(
                        "SELECT fileName, storageSiteRef, resourceUri, username, password FROM pdri_table "
                        + "JOIN storage_site_table ON storageSiteRef = storageSiteId "
                        + "JOIN credential_table ON credentialRef = credintialId "
                        + "WHERE pdriGroupRef = " + groupId);
                
                while (rs2.next()) {

                    PDRIDescr pdriDescr = new PDRIDescr(
                            rs2.getString(1),
                            rs2.getLong(2),
                            rs2.getString(3),
                            rs2.getString(4),
                            rs2.getString(5));
                    PDRI pdri = PDRIFactory.getFactory().createInstance(pdriDescr);
                    pdri.delete();
                }
                s2.executeUpdate("DELETE FROM pdri_table WHERE pdri_table.pdriId = " + groupId);
                rs1.deleteRow();
                rs1.beforeFirst();
            }
        } catch (SQLException | IOException e) {
            try {
                if (s1 != null && !s1.isClosed()) {
                    s1.close();
                    s1 = null;
                }
                if (s2 != null && !s2.isClosed()) {
                    s2.close();
                    s2 = null;
                }
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                DeleteSweep.log.log(Level.SEVERE, null, ex);
            }
            DeleteSweep.log.log(Level.SEVERE, null, e);
        } finally {
            try {
                if (s1 != null && !s1.isClosed()) {
                    s1.close();
                }
                if (s2 != null && !s2.isClosed()) {
                    s2.close();
                }
                if (connection != null && !connection.isClosed()) {
                    connection.commit();
                    connection.close();
                }
            } catch (SQLException ex) {
                DeleteSweep.log.log(Level.SEVERE, null, ex);
            }
        }


    }
}

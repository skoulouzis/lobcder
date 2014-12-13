package nl.uva.cs.lobcder.catalogue;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
class DeleteSweep implements Runnable {

    private final DataSource datasource;

    public DeleteSweep(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public void run() {
        PDRIFactory factory;
        try (Connection connection = datasource.getConnection()) {


            //Some times pdris are left in the pdriGrop with ref 1 while there is no refernace from the ldri. 
            //We'll try to sweep them as well
//                    ResultSet res4 = s1.executeQuery("update pdrigroup_table set `refCount` = 0 where pdriGroupId not in (select pdriGroupRef from ldata_table where pdriGroupRef != 0)");
            //TODO add it to the query below 
            //This query interferes with the assimilator
            try {
                connection.setAutoCommit(false);
                try (Statement s1 = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                java.sql.ResultSet.CONCUR_UPDATABLE)) {
                    ResultSet rs1 = s1.executeQuery("SELECT pdriGroupId FROM pdrigroup_table WHERE refCount = 0 ");
                    while (rs1.next()) {
                        Long groupId = rs1.getLong(1);
                        try (PreparedStatement ps2 = connection.prepareStatement(
                                        "SELECT pdriId, fileName, storageSiteRef FROM pdri_table WHERE pdriGroupRef = ?",
                                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                        java.sql.ResultSet.CONCUR_UPDATABLE)) {
                            ps2.setLong(1, groupId);
                            ResultSet rs2 = ps2.executeQuery();
                            while (rs2.next()) {
                                // here better to use a kind a local cache, i.e. hashmap
                                try (PreparedStatement ps3 = connection.prepareStatement(
                                                "SELECT resourceUri, username, password FROM storage_site_table "
                                                + "JOIN credential_table ON credentialRef = credintialId "
                                                + "WHERE storageSiteId = ?",
                                                java.sql.ResultSet.TYPE_FORWARD_ONLY,
                                                ResultSet.CONCUR_READ_ONLY)) {
                                    String fileName = rs2.getString(2);
                                    Long storageSiteRef = rs2.getLong(3);
                                    ps3.setLong(1, storageSiteRef);
                                    ResultSet rs3 = ps3.executeQuery();
                                    if (rs3.next()) {
                                        String resourceUri = rs3.getString(1);
                                        String username = rs3.getString(2);
                                        String password = rs3.getString(3);
                                        //For deleteing we don't care for encryption 
                                        PDRIDescr pdriDescr = new PDRIDescr(fileName, storageSiteRef, resourceUri, username, password, false, null, null, null);
                                        factory = PDRIFactory.getFactory();
                                        PDRI pdri = factory.createInstance(pdriDescr, false);
                                        DeleteSweep.log.log(Level.FINE, "PDRI Instance file name: {0}", new Object[]{pdri.getFileName()});
                                        pdri.delete();
                                        DeleteSweep.log.log(Level.FINE, "DELETE:", pdri.getURI());
                                        rs2.deleteRow();
                                        connection.commit();
                                    }
                                }
                            }
                            rs1.deleteRow();
                            connection.commit();
                        }
                    }
                }
            } catch ( Exception e) {
                if(e.getMessage().contains("No route to host")){
                     DeleteSweep.log.log(Level.WARNING, null, e);
                }else{
                    try {
                        throw e;
                    } catch (Exception ex) {
                        Logger.getLogger(DeleteSweep.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                connection.rollback();
            }
        } catch (SQLException e) {
            DeleteSweep.log.log(Level.SEVERE, null, e);
        }
    }
}

package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.PDRIFactory;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
class WP4Sweep implements Runnable {

    static final String uri = "http://vphshare.atosresearch.eu/metadata-retrieval/rest/metadata";

    private final DataSource datasource;

    public WP4Sweep(DataSource datasource) {
        this.datasource = datasource;
    }

    @Data
    @XmlRootElement(name = "resource_metadata")
    class ResourceMetadata {
        private String author;
        private Date creation_date;
        private String global_id;
        private int local_id;
        private String name;
        private String type;
        private Date updated_date;
        private int views;
    }

    @Override
    public void run() {
        PDRIFactory factory;
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE)) {
                Client client = Client.create();
                WebResource webResource = client.resource(uri);

                // looking for new entities
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, createDate, global_id, need_create FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocal_id(rs.getInt(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setCreation_date(rs.getDate(5));
                    rm.setUpdated_date(rm.getCreation_date());
                    rm.setGlobal_id(rs.getString(6));
                    rm.setViews(0);
                    try {
                        webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, rm);
                        rs.updateBoolean(7, false);
                        rs.updateRow();

                    } catch (Exception e) {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
                rs.close();
                // looking for entities to update
                rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, global_id, views, need_update FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocal_id(rs.getInt(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setCreation_date(rs.getDate(5));
                    rm.setUpdated_date(rs.getDate(6));
                    rm.setGlobal_id(rs.getString(7));
                    rm.setViews(rs.getInt(8));
                    try {
                        webResource = client.resource(uri.concat("/").concat(rm.getGlobal_id()));
                        webResource.type(MediaType.APPLICATION_XML).put(ClientResponse.class, rm);
                        rs.updateBoolean(9, false);
                        rs.updateRow();
                    } catch (Exception e) {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
                rs.close();
                // looking for removed entries
                rs = s1.executeQuery("SELECT global_id FROM wp4_table WHERE local_id IS NULL");
                while (rs.next()) {
                    try {
                        webResource = client.resource(uri.concat("/").concat(rs.getString(1)));
                        webResource.type(MediaType.APPLICATION_XML).delete();
                        rs.deleteRow();
                    } catch (Exception e) {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
                rs.close();
            }
        } catch (SQLException e) {
            WP4Sweep.log.log(Level.SEVERE, null, e);
        }
    }
}

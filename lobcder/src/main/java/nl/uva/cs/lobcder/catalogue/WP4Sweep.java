package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
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
    @XmlRootElement
    class resource_metadata{
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
            try {
                connection.setAutoCommit(false);
                try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_UPDATABLE)) {
                    Client client = Client.create();
                    WebResource r = client.resource(uri);

                    // looking for new entries
                    ResultSet rs1 = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, createDate, global_id, need_create FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE");
                    while (rs1.next()) {
                        resource_metadata rm = new resource_metadata();
                        rm.setLocal_id(rs1.getInt(1));
                        rm.setAuthor(rs1.getString(2));
                        rm.setType(rs1.getString(3));
                        rm.setName(rs1.getString(4));
                        rm.setCreation_date(rs1.getDate(5));
                        rm.setUpdated_date(rm.getCreation_date());
                        rm.setGlobal_id(rs1.getString(6);
                        rm.setViews(0);
                        try{
                            r.type(MediaType.APPLICATION_XML).post(ClientResponse.class,rm);
                        } catch (Exception e) {
                            WP4Sweep.log.log(Level.SEVERE, null, e);
                        }
                    }
                }
            } catch (SQLException | IOException e) {
                WP4Sweep.log.log(Level.SEVERE, null, e);
                connection.rollback();
            }
        } catch (SQLException e) {
            WP4Sweep.log.log(Level.SEVERE, null, e);
        }
    }
}

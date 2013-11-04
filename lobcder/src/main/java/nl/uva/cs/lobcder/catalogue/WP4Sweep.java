package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.extern.java.Log;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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


    public static class SqlTimestampAdapter extends XmlAdapter<java.util.Date, Timestamp> {

        @Override
        public java.util.Date marshal(Timestamp sqlDate) throws Exception {
            if(null == sqlDate) {
                return null;
            }
            return new java.util.Date(sqlDate.getTime());
        }

        @Override
        public java.sql.Timestamp unmarshal(java.util.Date utilDate) throws Exception {
            if(null == utilDate) {
                return null;
            }
            return new java.sql.Timestamp(utilDate.getTime());
        }

    }

    @Data
    @XmlRootElement(name = "resource_metadata")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResourceMetadata {
        private String author;
        @XmlJavaTypeAdapter(SqlTimestampAdapter.class)
        private Timestamp creation_date;
        private String global_id;
        private int local_id;
        private String name;
        private String type;
        @XmlJavaTypeAdapter(SqlTimestampAdapter.class)
        private Timestamp updated_date;
        private int views;
    }


    interface WP4ConnectorI {
        public void create(ResourceMetadata resourceMetadata) throws Exception;
        public void update(ResourceMetadata resourceMetadata) throws Exception;
        public void delete(String global_id) throws Exception;
    }

    class WP4Connector implements WP4ConnectorI{

        private Client client = Client.create();

        @Override
        public void create(ResourceMetadata resourceMetadata) {
            WebResource webResource = client.resource(uri);
            webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadata);
        }

        @Override
        public void update(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri.concat("/").concat(resourceMetadata.getGlobal_id()));
            webResource.type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata);
        }

        @Override
        public void delete(String global_id) throws Exception {
            WebResource webResource = client.resource(uri.concat("/").concat(global_id));
            webResource.type(MediaType.APPLICATION_XML).delete();
        }
    }

    class WP4ConnectorDebug implements WP4ConnectorI{
        @Override
        public void create(ResourceMetadata resourceMetadata) throws JAXBException {
            System.err.println("CREATE WP4 METADATA");
            JAXBContext context = JAXBContext.newInstance(ResourceMetadata.class);
            Marshaller m = context.createMarshaller();
            m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            m.marshal(resourceMetadata, System.err);
            System.err.println("================================");
        }

        @Override
        public void update(ResourceMetadata resourceMetadata) throws JAXBException{
            System.err.println("UPDATE WP4 METADATA");
            JAXBContext context = JAXBContext.newInstance(ResourceMetadata.class);
            Marshaller m = context.createMarshaller();
            m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            m.marshal(resourceMetadata, System.err);
            System.err.println("================================");
        }

        @Override
        public void delete(String global_id)  {
            System.err.println("DELETE WP4 METADATA");
            System.err.println(global_id);
            System.err.println("================================");
        }
    }

    private void create(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)) {
            try(PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create = FALSE WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, createDate, global_id, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocal_id(rs.getInt(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setCreation_date(rs.getTimestamp(5));
                    rm.setUpdated_date(rm.getCreation_date());
                    rm.setGlobal_id(rs.getString(6));
                    rm.setViews(0);
                    try {
                        wp4Connector.create(rm);
                        s2.setInt(1, rs.getInt(7));
                        s2.executeUpdate();
                    } catch (Exception e) {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
            }
        }
    }

    private void update(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)) {
            try(PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_update = FALSE WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, global_id, views, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocal_id(rs.getInt(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setCreation_date(rs.getTimestamp(5));
                    rm.setUpdated_date(rs.getTimestamp(6));
                    rm.setGlobal_id(rs.getString(7));
                    rm.setViews(rs.getInt(8));
                    try {
                        wp4Connector.update(rm);
                        s2.setInt(1, rs.getInt(9));
                        s2.executeUpdate();
                    } catch (Exception e) {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
            }
        }
    }

    private void delete(Connection connection, WP4ConnectorI wp4Connector) throws SQLException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE)) {
            ResultSet rs = s1.executeQuery("SELECT global_id FROM wp4_table WHERE local_id IS NULL");
            while (rs.next()) {
                try {
                    wp4Connector.delete(rs.getString(1));
                    rs.deleteRow();
                } catch (Exception e) {
                    WP4Sweep.log.log(Level.SEVERE, null, e);
                }
            }
        }
    }


    @Override
    public void run() {
        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            WP4ConnectorI wp4Connector = new WP4ConnectorDebug();
            create(connection, wp4Connector);
            update(connection, wp4Connector);
            delete(connection, wp4Connector);
        } catch (SQLException e) {
            WP4Sweep.log.log(Level.SEVERE, null, e);
        }
    }
}

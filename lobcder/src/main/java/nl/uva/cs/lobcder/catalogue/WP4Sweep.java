package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
class WP4Sweep implements Runnable {

    private final DataSource datasource;

    private final WP4ConnectorI connector;

    public WP4Sweep(DataSource datasource, WP4ConnectorI connector) {
        this.datasource = datasource;
        this.connector = connector;
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
        private Long local_id;
        private String name;
        private String type;
        @XmlJavaTypeAdapter(SqlTimestampAdapter.class)
        private Timestamp updated_date;
        private int views;
        private String category;
        private String description;
        private String licence;
        private String rating;
        private String semantic_annotations;
        private String status;
        private String tags;
    }



    public static interface WP4ConnectorI {
        public String create(ResourceMetadata resourceMetadata) throws Exception;
        public void update(ResourceMetadata resourceMetadata) throws Exception;
        public void delete(String global_id) throws Exception;
    }

    public static class WP4Connector implements WP4ConnectorI{

        private Client client;
        private XPathExpression expression;
        private final String uri;

        @SneakyThrows
        public WP4Connector(String uri) {
            this.uri = uri;
            client = Client.create();
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            expression = xpath.compile("/message/data[1]/_global_id[1]");
        }

        @Override
        public String create(ResourceMetadata resourceMetadata) throws Exception{
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadata);
            if(response.getClientResponseStatus() == ClientResponse.Status.OK){
                Node uidNode = (Node) expression.evaluate(new InputSource(response.getEntityInputStream()), XPathConstants.NODE);
                return uidNode.getTextContent();
            } else {
                throw new Exception(response.getClientResponseStatus().toString());
            }
        }

        @Override
        public void update(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.path(resourceMetadata.getGlobal_id()).type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata);
            if(response.getClientResponseStatus() != ClientResponse.Status.OK){
                throw new Exception(response.getClientResponseStatus().toString());
            }
        }

        @Override
        public void delete(String global_id) throws Exception {
            WebResource webResource = client.resource(uri);
            webResource.path(global_id).type(MediaType.APPLICATION_XML).delete();
        }
    }

    public static class WP4ConnectorDebug implements WP4ConnectorI{
        @Override
        public String create(ResourceMetadata resourceMetadata) throws JAXBException {
            System.err.println("CREATE WP4 METADATA");
            JAXBContext context = JAXBContext.newInstance(ResourceMetadata.class);
            Marshaller m = context.createMarshaller();
            m.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            m.marshal(resourceMetadata, System.err);
            System.err.println("================================");
            return UUID.randomUUID().toString();
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
            try(PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create = FALSE, global_id = ? WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocal_id(Long.valueOf(rs.getLong(1)));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setViews(0);
                    rm.setCategory("");
                    rm.setDescription("");
                    rm.setLicence("");
                    rm.setRating("");
                    rm.setSemantic_annotations("");
                    rm.setTags("LOBCDER");
                    try {
                        String res = wp4Connector.create(rm);
                        s2.setString(1, res);
                        s2.setLong(2, rs.getLong(5));
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
                ResultSet rs = s1.executeQuery("SELECT ownerId, ldName, global_id, views, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setAuthor(rs.getString(1));
                    rm.setName(rs.getString(2));
                    rm.setGlobal_id(rs.getString(3));
                    rm.setViews(rs.getInt(4));
                    try {
                        wp4Connector.update(rm);
                        s2.setLong(1, rs.getLong(5));
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
            ResultSet rs = s1.executeQuery("SELECT global_id, id FROM wp4_table WHERE local_id IS NULL");
            while (rs.next()) {
                try {
                    String global_id = rs.getString(1);
                    if(global_id != null)
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
            create(connection, connector);
            update(connection, connector);
            delete(connection, connector);
        } catch (SQLException e) {
            WP4Sweep.log.log(Level.SEVERE, null, e);
        }
    }
}

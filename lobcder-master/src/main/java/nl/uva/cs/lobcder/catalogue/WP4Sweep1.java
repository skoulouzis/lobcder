package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: dvasunin
 * Date: 13.02.2015
 * Time: 14:24
 * To change this template use File | Settings | File Templates.
 */
public class WP4Sweep1 implements Runnable {
    private final DataSource datasource;
    private final String metadataRepository;

    public WP4Sweep1(DataSource datasource) throws IOException {
        this.datasource = datasource;
        metadataRepository = PropertiesHelper.getMetadataRepositoryURL();
    }

    static enum FileType{
        File, Folder;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    static class FileWP4{
        @Setter private String author;
        @Getter @Setter private String globalID;
        private String category = "General Metadata";
        private String description = "LOBCDER_TEST1";
        private String linkedTo ="";
        @Getter @Setter private Long localID;
        @Setter private String name;
        private Long rating=0L;
        private String relatedResources="";
        private String semanticAnnotations="";
        private String status="active";
        private String type = "File";
        @Setter private Integer views;
        @Setter private FileType fileType;
        private String subjectID ="";
    }

    @XmlRootElement(name = "resource_metadata")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ResourceMetadata{
        @XmlElement(name = "file")
        @Delegate
        private FileWP4 file = new FileWP4();
    }

    @XmlRootElement(name = "resource_metadata_list")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ResourceMetadataList{
        @XmlElement(name = "resource_metadata")
        @Getter @Setter private Collection<ResourceMetadata> resourceMetadataList;
    }


    private static String getGlobalIdForDelete(Collection<String> globalIdCollection) {
        StringBuilder sb = new StringBuilder();
        sb.append("<globalID_list>");
        for(String id : globalIdCollection){
            sb.append(id).append(",");
        }
        sb.replace(sb.lastIndexOf(","), sb.length(), "");
        sb.append("</globalID_list>");
        return sb.toString();
    }

    public static interface WP4ConnectorI {

        public ResourceMetadataList create(ResourceMetadataList resourceMetadataList) throws Exception;

        public ResourceMetadataList update(ResourceMetadataList resourceMetadataList) throws Exception;

        public ResourceMetadataList delete(Collection<String> globalIdCollection) throws Exception;

    }

    public static class WP4Connector implements WP4ConnectorI {
        private Client client;
        private final String uri;

        public WP4Connector(String uri) {
            this.uri = uri;
        }

        @Override
        public ResourceMetadataList create(ResourceMetadataList resourceMetadataList) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadataList);
            if(response.getClientResponseStatus() != ClientResponse.Status.OK){
                throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + response.getEntity(String.class));
            }
            return response.getEntity(ResourceMetadataList.class);
        }

        @Override
        public ResourceMetadataList update(ResourceMetadataList resourceMetadataList) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadataList);
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + response.getEntity(String.class));
            }
            return response.getEntity(ResourceMetadataList.class);
        }

        @Override
        public ResourceMetadataList delete(Collection<String> globalIdCollection) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).delete(ClientResponse.class, getGlobalIdForDelete(globalIdCollection));
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new Exception(uri + " responded with: " + response.getClientResponseStatus().toString() + ". Response Entity:" + response.getEntity(String.class));
            }
            return response.getEntity(ResourceMetadataList.class);
        }
    }


    private void create(Connection connection, WP4ConnectorI wp4Connector) throws Exception {
        Collection<ResourceMetadata> resourceMetadataList;
        Map<Long, Long> resourceMetadataMap;
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE LIMIT 1000");
            int size = rs.getFetchSize();
            resourceMetadataList = new ArrayList<>(size);
            resourceMetadataMap = new HashMap<>(size);
            while (rs.next()) {
                ResourceMetadata rm = new ResourceMetadata();
                Long localId = rs.getLong(1);
                rm.setLocalID(localId);
                rm.setAuthor(rs.getString(2));
                rm.setFileType(rs.getString(3).equals("logical.file") ? FileType.File : FileType.Folder);
                rm.setName(rs.getString(4));
                resourceMetadataMap.put(localId, rs.getLong(5));
                resourceMetadataList.add(rm);
            }
        }
        try(PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create=FALSE, global_id=? WHERE id=?")) {
            ResourceMetadataList param = new ResourceMetadataList();
            param.setResourceMetadataList(resourceMetadataList);
            for(ResourceMetadata rm : wp4Connector.create(param).getResourceMetadataList()) {
                s2.setString(1, rm.getGlobalID());
                s2.setLong(2, resourceMetadataMap.get(rm.getLocalID()));
                s2.addBatch();
            }
            s2.executeBatch();
        }
    }

    private void update(Connection connection, WP4ConnectorI wp4Connector) throws Exception {
        Collection<ResourceMetadata> resourceMetadataList;
        Map<Long, Long> resourceMetadataMap;
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT ownerId, ldName, global_id, views, local_id, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE LIMIT 1000");
            int size = rs.getFetchSize();
            resourceMetadataList = new ArrayList<>(size);
            resourceMetadataMap = new HashMap<>(size);
            while (rs.next()) {
                ResourceMetadata rm = new ResourceMetadata();
                rm.setAuthor(rs.getString(1));
                rm.setName(rs.getString(2));
                rm.setGlobalID(rs.getString(3));
                rm.setViews(rs.getInt(4));
                Long localId = rs.getLong(5);
                rm.setLocalID(localId);
                resourceMetadataMap.put(localId, rs.getLong(6));
                resourceMetadataList.add(rm);
            }
        }
        try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_update=FALSE WHERE id=?")) {
            ResourceMetadataList param = new ResourceMetadataList();
            param.setResourceMetadataList(resourceMetadataList);
            for(ResourceMetadata rm : wp4Connector.update(param).getResourceMetadataList()) {
                s2.setLong(1, resourceMetadataMap.get(rm.getLocalID()));
                s2.addBatch();
            }
            s2.executeBatch();
        }
    }

    private void delete(Connection connection, WP4ConnectorI wp4Connector) throws Exception {
        Map<String, Long> deleteMetadataMap;
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = s1.executeQuery("SELECT global_id, id FROM wp4_table WHERE local_id IS NULL LIMIT 1000");
            int size = rs.getFetchSize();
            deleteMetadataMap = new HashMap<>(size);
            while (rs.next()) {
                deleteMetadataMap.put(rs.getString(1), rs.getLong(2));
            }
        }
        try (PreparedStatement s2 = connection.prepareStatement("DELETE FROM wp4_table WHERE id=?")) {
            for (ResourceMetadata rm : wp4Connector.delete(deleteMetadataMap.keySet()).getResourceMetadataList()) {
                s2.setLong(1, deleteMetadataMap.get(rm.getGlobalID()));
                s2.addBatch();
            }
            s2.executeBatch();
        }
    }

    @Override
    public void run() {

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            WP4ConnectorI connector = new WP4Sweep1.WP4Connector(metadataRepository);
            create(connection, connector);
            update(connection, connector);
            delete(connection, connector);
        } catch (Exception ex) {
            Logger.getLogger(WP4Sweep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void serialize(ResourceMetadataList rml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMetadataList.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        //Marshal the employees list in console
        jaxbMarshaller.marshal(rml, System.out);
    }

    public ResourceMetadataList unmarchaling() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ResourceMetadataList.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        //We had written this file in marshalling example
        return (ResourceMetadataList) jaxbUnmarshaller.unmarshal( new File("C:\\tmp\\test.xml") );


    }

    public static void main(String arg[]){
        try {
            WP4Sweep1 wp4 = new WP4Sweep1(null);
/*
            List<ResourceMetadata> resourceMetadataList = new ArrayList<>();
            ResourceMetadata rm1 = new ResourceMetadata();
            rm1.setName("File1");
            rm1.setLocalId(42L);
            rm1.setType(FileType.File);
            resourceMetadataList.add(rm1);

            ResourceMetadata rm2 = new ResourceMetadata();
            rm2.setName("Folder2");
            rm2.setLocalId(35L);
            rm2.setType(FileType.Folder);
            resourceMetadataList.add(rm2);

            ResourceMetadataList rml = new ResourceMetadataList();
            rml.setResourceMetadataList(resourceMetadataList);
  */          wp4.serialize(wp4.unmarchaling());

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}

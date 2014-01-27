/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**
 *
 * @author S. koulouzis
 */
public class TestREST {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;
    private String testres1;
    private String testres2;
    private String testcol;
    private String restURL;
    private Client restClient;
    private String testResourceId;
    private String translatorURL;

    @Before
    public void setUp() throws Exception {
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder-tests"
//                + File.separator + "etc" + File.separator + "test.proprties";
        String propBasePath = "etc" + File.separator + "test.proprties";

        Properties prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-2.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }

        this.uri = URI.create(testURL);
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }

        this.username = prop.getProperty(("webdav.test.username1"), "");
        if (username == null) {
            username = "user";
        }
        assertTrue(username != null);
        this.password = prop.getProperty(("webdav.test.password1"), "");
        if (password == null) {
            password = "token0";
        }
        assertTrue(password != null);

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }

        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));

        restURL = prop.getProperty(("rest.test.url"), "http://localhost:8080/lobcder/rest/");
        testResourceId = "testResourceId";
        testcol = this.root + testResourceId + "/";

        translatorURL = prop.getProperty(("translator.test.url"), "http://localhost:8080/lobcder/urest/");



        ClientConfig clientConfig = configureClient();
//        SSLContext ctx = SSLContext.getInstance("SSL");
//        clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, ctx));
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        restClient = Client.create(clientConfig);
        restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username, password));

    }

    @After
    public void tearDown() throws Exception {
    }

    private void createCollection() throws IOException {
        MkColMethod mkcol = new MkColMethod(testcol);
        int status = this.client.executeMethod(mkcol);
        assertEquals(HttpStatus.SC_CREATED, status);


        PutMethod put = new PutMethod(this.root + testResourceId + "/file1");
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        status = this.client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        //Are you sure it's there ????
        GetMethod get = new GetMethod(this.root + testResourceId + "/file1");
        status = client.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals("foo", get.getResponseBodyAsString());

//        put = new PutMethod(this.root + "testResourceId/file2");
//        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
//        status = this.client.executeMethod(put);
//        assertEquals(HttpStatus.SC_CREATED, status);
    }

    private void deleteCollection() throws IOException {
        DeleteMethod delete = new DeleteMethod(testcol);
        int status = this.client.executeMethod(delete);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testQueryItems() throws IOException {
        System.err.println("testQueryItems");
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/" + testResourceId);

            WebResource res = webResource.path("items").path("query").queryParams(params);
//            ClientResponse response = res.put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);            

            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            LogicalDataWrapped logicalDataWrapped = null;
            for (LogicalDataWrapped ldw : list) {
                checkLogicalDataWrapped(ldw);
                if (ldw.path.equals("/" + testResourceId) && ldw.logicalData.type.equals("logical.folder")) {
                    logicalDataWrapped = ldw;
                    break;
                }
            }
            assertNotNull(logicalDataWrapped);
            for (Permissions p : logicalDataWrapped.permissions) {
                assertEquals(username, p.owner);
                assertTrue(p.read.contains("admin"));
//                for (String s : p.read) {
//                    System.err.println("Read:" + s);
//                }
                assertNull(p.write);
//                for (String s : p.write) {
//                    System.err.println("write:" + s);
//                }
            }

        } finally {
            deleteCollection();
        }
    }

    private void checkLogicalDataWrapped(LogicalDataWrapped ldw) {

        assertNotNull(ldw.path);
        assertTrue((ldw.logicalData.uid != 0));
        if (ldw.logicalData.type.equals("logical.file")) {
            assertTrue((ldw.logicalData.pdriGroupId != 0));
            assertFalse(ldw.pdriList.isEmpty());
            assertNotNull(ldw.logicalData.contentTypesAsString);
            for (PDRIDesc pdri : ldw.pdriList) {
                assertNotNull(pdri.name);
                assertNotNull(pdri.password);
                assertNotNull(pdri.resourceUrl);
                assertNotNull(pdri.username);
            }
        }

        assertNotNull(ldw.logicalData.createDate);
        assertTrue((ldw.logicalData.createDate != 0));
        assertNotNull(ldw.logicalData.modifiedDate);
        assertTrue((ldw.logicalData.modifiedDate != 0));
        assertNotNull(ldw.logicalData.name);

        for (Permissions perm : ldw.permissions) {
            assertNotNull(perm.owner);
        }
    }

    @Test
    public void testQueryItem() throws IOException {
        System.err.println("testQueryItem");
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            LogicalDataWrapped logicalDataWrapped = null;
            for (LogicalDataWrapped lwd : list) {
                if (lwd.logicalData.type.equals("logical.file") && lwd.logicalData.name.equals("file1")) {
                    logicalDataWrapped = lwd;
                }
            }

            assertNotNull(logicalDataWrapped);
            assertFalse(logicalDataWrapped.logicalData.supervised);
//            assertEquals(logicalDataWrapped.logicalData.parent, "/testResourceId");
            assertEquals("text/plain; charset=UTF-8", logicalDataWrapped.logicalData.contentTypesAsString);

            //Get the uid 
            int fileUID = logicalDataWrapped.logicalData.uid;
            res = webResource.path("item").path("query").path(String.valueOf(fileUID));
            LogicalDataWrapped theFile = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<LogicalDataWrapped>() {
            });
            assertEquals(fileUID, theFile.logicalData.uid);
            assertNotNull(theFile);


            assertEquals(theFile.logicalData.type, "logical.file");
            for (Permissions p : theFile.permissions) {
                assertEquals(p.owner, username);
                assertTrue(p.read.contains("admin"));
//                for (String s : p.read) {
//                    System.err.println("Read:" + s);
//                }
                assertNull(p.write);
//                for (String s : p.write) {
//                    System.err.println("write:" + s);
//                }
            }
            assertEquals(theFile.logicalData.name, "file1");
            assertFalse(theFile.logicalData.supervised);
//            assertEquals(theFile.logicalData.parent, "/testResourceId");
            assertEquals("text/plain; charset=UTF-8", theFile.logicalData.contentTypesAsString);

        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testDataItem() throws IOException {
        System.err.println("testDataItem");
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            LogicalDataWrapped logicalDataWrapped = null;
            for (LogicalDataWrapped ldw : list) {
                checkLogicalDataWrapped(ldw);
                if (ldw.logicalData.type.equals("logical.file") && ldw.logicalData.name.equals("file1")) {
                    logicalDataWrapped = ldw;
                }
            }
            assertNotNull(logicalDataWrapped);
            for (Permissions p : logicalDataWrapped.permissions) {
                assertEquals(p.owner, username);
                assertTrue(p.read.contains("admin"));
//                for (String s : p.read) {
//                    System.err.println("Read:" + s);
//                }
                assertNull(p.write);
//                for (String s : p.write) {
//                    System.err.println("write:" + s);
//                }
            }
            assertFalse(logicalDataWrapped.logicalData.supervised);
//            assertEquals(logicalDataWrapped.logicalData.parent, "/testResourceId");
            assertEquals("text/plain; charset=UTF-8", logicalDataWrapped.logicalData.contentTypesAsString);

//            //Get the uid 
//            int fileUID = logicalDataWrapped.logicalData.uid;
//            res = webResource.path("item").path("data").path(String.valueOf(fileUID));
//            ClientResponse response = res.get(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
//            InputStream ins = response.getEntityInputStream();
//            byte[] d = new byte[3];
//            ins.read(d);
//            ins.close();
//            assertEquals(new String(d), "foo");

        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testReservation() throws IOException {
        System.err.println("testReservation");
        try {
            createCollection();
            //Wait for replication
            Thread.sleep(15000);


            // /rest/reservation/get_workers/?id=all
            WebResource webResource = restClient.resource(restURL);

            //Get list of workers 
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("id", "all");
            WebResource res = webResource.path("reservation").path("get_workers").queryParams(params);
            List<WorkerStatus> workersList = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<WorkerStatus>>() {
            });


            //If we have workers ask for a path reservation 
            if (workersList != null && workersList.size() > 0) {
                //rest/reservation/5455/request/?dataPath=/&storageSiteHost=sps1&storageSiteHost=sps2&storageSiteHost=sps3
                params = new MultivaluedMapImpl();
                String dataPath = "file1";
                params.add("dataName", dataPath);
                for (WorkerStatus w : workersList) {
                    params.add("storageSiteHost", w.hostName);
                }

                res = webResource.path("reservation").path("some_communication_id").path("request").queryParams(params);
                ReservationInfo info = res.accept(MediaType.APPLICATION_XML).
                        get(new GenericType<ReservationInfo>() {
                });

                assertNotNull(info);
                assertNotNull(info.communicationID);
                assertNotNull(info.storageHost);
                assertNotNull(info.storageHostIndex);
                assertNotNull(info.workerDataAccessURL);


                //Check if worker is ready 
                params = new MultivaluedMapImpl();
                params.add("host", info.storageHost);


                res = webResource.path("reservation").path("workers").queryParams(params);
                List<WorkerStatus> list = res.accept(MediaType.APPLICATION_XML).
                        get(new GenericType<List<WorkerStatus>>() {
                });

                assertNotNull(list);
                assertFalse(list.isEmpty());
                for (WorkerStatus w : list) {
                    assertNotNull(w.status);
                    assertNotNull(w.hostName);
                    assertEquals("READY", w.status);
                }

                //Now get the file 
                GetMethod get = new GetMethod(info.workerDataAccessURL);
                int status = client.executeMethod(get);
                assertEquals(HttpStatus.SC_OK, status);
                assertEquals("foo", get.getResponseBodyAsString());




                //run without host names 
                params = new MultivaluedMapImpl();
                dataPath = "file1";
                params.add("dataName", dataPath);
                res = webResource.path("reservation").path("some_communication_id").path("request").queryParams(params);
                info = res.accept(MediaType.APPLICATION_XML).
                        get(new GenericType<ReservationInfo>() {
                });

                assertNotNull(info);
                assertNotNull(info.communicationID);
                assertNotNull(info.storageHostIndex);
                assertNotNull(info.workerDataAccessURL);


                //Now get the file 
                get = new GetMethod(info.workerDataAccessURL);
                status = client.executeMethod(get);
                assertEquals(HttpStatus.SC_OK, status);
                assertEquals("foo", get.getResponseBodyAsString());

            }
        } catch (Exception ex) {
            Logger.getLogger(TestREST.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testGetWorkersStatus() throws IOException {
        System.err.println("testGetWorkersStatus");
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);
//        rest/reservation/workers/?host=kscvdfv&host=sp2&host=192.168.1.1
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("host", "host1");
            params.add("host", "host2");
            params.add("host", "host3");

            WebResource res = webResource.path("reservation").path("workers").queryParams(params);
            List<WorkerStatus> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<WorkerStatus>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            for (WorkerStatus w : list) {
                assertNotNull(w.status);
                assertNotNull(w.hostName);
            }


        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testTicketTranslator() throws IOException {
        System.err.println("testTicketTranslator");
        try {
            createCollection();

            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            Client nonAuthRestClient = Client.create(clientConfig);

            WebResource webResource = nonAuthRestClient.resource(translatorURL);
            WebResource res = webResource.path("getshort").path(password);

            String shortToken = res.accept(MediaType.TEXT_PLAIN).get(String.class);
            assertNotNull(shortToken);


            Client shortAuthRestClient = Client.create(clientConfig);
            shortAuthRestClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username, shortToken));




            webResource = shortAuthRestClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());

        } finally {
            deleteCollection();
        }
    }

    public static ClientConfig configureClient() {
        TrustManager[] certs = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }
            }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        ClientConfig config = new DefaultClientConfig();
        try {
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    },
                    ctx));
        } catch (Exception e) {
        }
        return config;
    }

    @XmlRootElement
    public static class LogicalDataWrapped {

        public LogicalData logicalData;
        public String path;
        public Set<PDRIDesc> pdriList;
        public Set<Permissions> permissions;
    }

    @XmlRootElement
    public static class LogicalData {

        public int checksum;
        public String contentTypesAsString;
        public long createDate;
        public long lastValidationDate;
        public long length;
        public int lockTimeout;
        public long modifiedDate;
        public String name;
        public String owner;
        public int parentRef;
        public int pdriGroupId;
        public boolean supervised;
        public String type;
        public int uid;
    }

    @XmlRootElement
    public static class Permissions {

        public String owner;
        public Set<String> read;
        public Set<String> write;
    }

    @XmlRootElement
    public static class PDRIDesc {

        public String name;
        public String password;
        public String resourceUrl;
        public String username;
    }

    @XmlRootElement
    public static class ReservationInfo {

        @XmlElement(name = "communicationID")
        private String communicationID;
        @XmlElement(name = "storageHost")
        private String storageHost;
        @XmlElement(name = "storageHostIndex")
        private int storageHostIndex;
        @XmlElement(name = "workerDataAccessURL")
        private String workerDataAccessURL;
    }

    @XmlRootElement
    public static class WorkerStatus {

        @XmlElement(name = "hostName")
        private String hostName;
        @XmlElement(name = "status")
        private String status;
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


import javax.xml.bind.*;
import javax.xml.namespace.QName;


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
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.apache.jackrabbit.webdav.DavException;

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
//    private String testcol;
    private String restURL;
    private Client restClient;
//    private String testResourceId;
    private String translatorURL;
    private String mrURL;
    private Utils utils;
    private Boolean quckTest;

    @Before
    public void setUp() throws Exception {
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder-tests"
//                + File.separator + "etc" + File.separator + "test.properties";
        String propBasePath = "etc" + File.separator + "test.properties";

        Properties prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url", "http://localhost:8080/lobcder/dav");
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }

        this.uri = URI.create(testURL);
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }

        this.username = prop.getProperty(("webdav.test.username1"), "user");
        assertTrue(username != null);
        this.password = prop.getProperty(("webdav.test.password1"), "token0");
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
//        testResourceId = "testResourceId";
//        testcol = this.root + testResourceId + "/";

        translatorURL = prop.getProperty(("translator.test.url"), "http://localhost:8080/lobcder/urest/");

        mrURL = prop.getProperty(("metadata.repository.url"), "http://vphshare.atosresearch.eu/metadata-extended/rest/metadata");

        quckTest = Boolean.valueOf(prop.getProperty(("test.quick"), "true"));



        ClientConfig clientConfig = configureClient();
//        SSLContext ctx = SSLContext.getInstance("SSL");
//        clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, ctx));
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        restClient = Client.create(clientConfig);
        restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username, password));


        utils = new Utils(client);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPermissionsManyOwners() throws IOException, DavException {
        System.err.println("testPermissionsManyOwners");
        String testcolName = "testPermissionsManyOwners";
        String rootcol = root + testcolName + "/";
        String owner1col = rootcol + "owner1";
        String owner2col = rootcol + "owner2";

        String owner1Subcol = owner1col + "/sub";
        String owner2Subcol = owner2col + "/sub";

        String owner1SubSub = owner1Subcol + "/sub";
        String owner2SubSub = owner2Subcol + "/sub";
        try {
            utils.createCollection(rootcol, true);
            utils.createCollection(owner1col, true);
            utils.createCollection(owner2col, true);
            utils.createCollection(owner1Subcol, true);
            utils.createCollection(owner2Subcol, true);
            utils.createCollection(owner1SubSub, true);
            utils.createCollection(owner2SubSub, true);

            utils.createFile(owner1col + "file", true);
            utils.createFile(owner2col + "/file", true);
            utils.createFile(owner1Subcol + "/file", true);
            utils.createFile(owner2Subcol + "/file", true);
            utils.createFile(owner1SubSub + "/file", true);
            utils.createFile(owner2SubSub + "/file", true);



            WebResource webResource = restClient.resource(restURL);
            Long uid = utils.getResourceUID(rootcol);
            WebResource res = webResource.path("item").path("permissions").path(String.valueOf(uid));
            Permissions perm = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<Permissions>() {
            });
            assertNotNull(perm);
            assertNotNull(perm.read);
            assertNotNull(perm.write);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName);

            res = webResource.path("items").path("query").queryParams(params);

            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            utils.checkPermissions(list, perm);





            Permissions owner1 = new Permissions();
            owner1.owner = "owner1";
            Set<String> canRead = new HashSet();
            canRead.add(owner1.owner);
            Set<String> canWrite = new HashSet();
            canWrite.add(owner1.owner);
            owner1.read = canRead;
            owner1.write = canWrite;

            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName + "/owner1");
            res = webResource.path("items").path("permissions").queryParams(params);

            ClientResponse response = res.put(ClientResponse.class, owner1);
            assertEquals(response.getStatus(), HttpStatus.SC_NO_CONTENT);





            Permissions owner2 = new Permissions();
            owner2.owner = "owner2";
            canRead = new HashSet();
            canRead.add(owner2.owner);
            canWrite = new HashSet();
            canWrite.add(owner2.owner);
            owner2.read = canRead;
            owner2.write = canWrite;

            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName + "/owner2");
            res = webResource.path("items").path("permissions").queryParams(params);
            response = res.put(ClientResponse.class, owner2);
            assertEquals(response.getStatus(), HttpStatus.SC_NO_CONTENT);


            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName + "/owner1");
            res = webResource.path("items").path("query").queryParams(params);


            List<LogicalDataWrapped> listOwner1 = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            utils.checkPermissions(listOwner1, owner1);



            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName + "/owner2");
            res = webResource.path("items").path("query").queryParams(params);


            List<LogicalDataWrapped> listOwner2 = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            utils.checkPermissions(listOwner2, owner2);


        } finally {
            utils.deleteResource(rootcol, false);
        }
    }

    @Test
    public void testPermissions() throws IOException, DavException {
        System.err.println("testPermissions");
        String testcolName = "testResourceForPermissions";
        String testcol = root + testcolName + "/";
        String testcol1 = testcol + "sub1";
        String testcol2 = testcol + "sub2";
        String testcol3 = testcol2 + "/sub1";
        String testcol4 = testcol3 + "/sub3";

        try {
            utils.createCollection(testcol, true);
            utils.createCollection(testcol1, true);
            utils.createCollection(testcol2, true);
            utils.createCollection(testcol3, true);
            utils.createCollection(testcol4, true);

            utils.createFile(testcol + "file", true);
            utils.createFile(testcol1 + "/file", true);
            utils.createFile(testcol2 + "/file", true);
            utils.createFile(testcol3 + "/file", true);
            utils.createFile(testcol4 + "/file", true);


            WebResource webResource = restClient.resource(restURL);
            Long uid = utils.getResourceUID(testcol);
            WebResource res = webResource.path("item").path("permissions").path(String.valueOf(uid));
            Permissions perm = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<Permissions>() {
            });
            assertNotNull(perm);
            assertNotNull(perm.read);
            assertNotNull(perm.write);
            String initialOwner = perm.owner;
            Set<String> initialRead = perm.read;
            String[] initialReadArray = initialRead.toArray(new String[initialRead.size()]);
            Set<String> initialWrite = perm.write;
            String[] initialWriteArray = initialWrite.toArray(new String[initialWrite.size()]);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName);

            res = webResource.path("items").path("query").queryParams(params);

            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            utils.checkPermissions(list, perm);

//            http://host.com/lobcder/rest/items/permissions?path={path}
            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName);

            res = webResource.path("items").path("permissions").queryParams(params);


            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName);

            Permissions newPerm = new Permissions();
            newPerm.owner = "someNewOnwer";
            Set<String> canRead = new HashSet();
            canRead.add(newPerm.owner);
            Set<String> canWrite = new HashSet();
            canWrite.add(newPerm.owner);
            newPerm.read = canRead;
            newPerm.write = canWrite;

            String[] newPermReadArray = newPerm.read.toArray(new String[newPerm.read.size()]);
            String[] newPermWriteArray = newPerm.write.toArray(new String[newPerm.write.size()]);

            ClientResponse response = res.put(ClientResponse.class, newPerm);
            assertEquals(response.getStatus(), HttpStatus.SC_NO_CONTENT);

            params = null;
            params = new MultivaluedMapImpl();
            params.add("path", "/" + testcolName);
            res = webResource.path("items").path("query").queryParams(params);

            list = null;
            list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            utils.checkPermissions(list, newPerm);

        } finally {
            utils.deleteResource(testcol, false);
        }

    }

    @Test
    public void testQueryItems() throws IOException {
        System.err.println("testQueryItems");
        String testcol = root + "testResourceForQueryItems/";
        try {
            utils.createCollection(testcol, true);
            utils.createFile(this.root + "testResourceForQueryItems" + "/file1", true);
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceForQueryItems");

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
                utils.checkLogicalDataWrapped(ldw);
                if (ldw.path.equals("/testResourceForQueryItems") && ldw.logicalData.type.equals("logical.folder")) {
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
//                assertNull(p.write);
//                for (String s : p.write) {
//                    System.err.println("write:" + s);
//                }
            }

        } finally {
            utils.deleteResource(testcol, false);
        }
    }

    @Test
    public void testQueryItem() throws IOException {
        System.err.println("testQueryItem");
        String testcol = root + "testResourceForQueryItem/";
        String testURI1 = testcol + "file1";
        try {

            utils.createCollection(testcol, true);
            utils.createFile(this.root + testURI1, true);

            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceForQueryItem");

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
//                assertNull(p.write);
//                for (String s : p.write) {
//                    System.err.println("write:" + s);
//                }
            }
            assertEquals(theFile.logicalData.name, "file1");
            assertFalse(theFile.logicalData.supervised);
//            assertEquals(theFile.logicalData.parent, "/testResourceId");
            assertEquals("text/plain; charset=UTF-8", theFile.logicalData.contentTypesAsString);

        } finally {
            utils.deleteResource(testcol, false);
        }
    }

    @Test
    public void testDataItem() throws IOException {
        System.err.println("testDataItem");
        String testcol = root + "testResourceForDataItem/";
        String testURI1 = testcol + "file1";
        try {
            utils.createCollection(testcol, true);
            utils.createFile(this.root + testURI1, true);
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceForDataItem");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            LogicalDataWrapped logicalDataWrapped = null;
            for (LogicalDataWrapped ldw : list) {
                utils.checkLogicalDataWrapped(ldw);
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
//                assertNull(p.write);
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
            utils.deleteResource(testcol, false);
        }
    }

//    @Test
//    public void testReservation() throws IOException {
//        System.err.println("testReservation");
//        try {
//            createCollection();
//            //Wait for replication
//            Thread.sleep(15000);
//
//
//            // /rest/reservation/get_workers/?id=all
//            WebResource webResource = restClient.resource(restURL);
//
//            //Get list of workers 
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("id", "all");
//            WebResource res = webResource.path("reservation").path("get_workers").queryParams(params);
//            List<WorkerStatus> workersList = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<WorkerStatus>>() {
//            });
//
//
//            //If we have workers ask for a path reservation 
//            if (workersList != null && workersList.size() > 0) {
//                //rest/reservation/5455/request/?dataPath=/&storageSiteHost=sps1&storageSiteHost=sps2&storageSiteHost=sps3
//                params = new MultivaluedMapImpl();
//                String dataPath = "file1";
//                params.add("dataName", dataPath);
//                for (WorkerStatus w : workersList) {
//                    params.add("storageSiteHost", w.hostName);
//                }
//
//                res = webResource.path("reservation").path("some_communication_id").path("request").queryParams(params);
//                ReservationInfo info = res.accept(MediaType.APPLICATION_XML).
//                        get(new GenericType<ReservationInfo>() {
//                });
//
//                assertNotNull(info);
//                assertNotNull(info.communicationID);
//                assertNotNull(info.storageHost);
//                assertNotNull(info.storageHostIndex);
//                assertNotNull(info.workerDataAccessURL);
//
//
//                //Check if worker is ready 
//                params = new MultivaluedMapImpl();
//                params.add("host", info.storageHost);
//
//
//                res = webResource.path("reservation").path("workers").queryParams(params);
//                List<WorkerStatus> list = res.accept(MediaType.APPLICATION_XML).
//                        get(new GenericType<List<WorkerStatus>>() {
//                });
//
//                assertNotNull(list);
//                assertFalse(list.isEmpty());
//                for (WorkerStatus w : list) {
//                    assertNotNull(w.status);
//                    assertNotNull(w.hostName);
//                    assertEquals("READY", w.status);
//                }
//
//                //Now get the file 
//                GetMethod get = new GetMethod(info.workerDataAccessURL);
//                int status = client.executeMethod(get);
//                assertEquals(HttpStatus.SC_OK, status);
//                assertEquals("foo", get.getResponseBodyAsString());
//
//
//
//
//                //run without host names 
//                params = new MultivaluedMapImpl();
//                dataPath = "file1";
//                params.add("dataName", dataPath);
//                res = webResource.path("reservation").path("some_communication_id").path("request").queryParams(params);
//                info = res.accept(MediaType.APPLICATION_XML).
//                        get(new GenericType<ReservationInfo>() {
//                });
//
//                assertNotNull(info);
//                assertNotNull(info.communicationID);
//                assertNotNull(info.storageHostIndex);
//                assertNotNull(info.workerDataAccessURL);
//
//
//                //Now get the file 
//                get = new GetMethod(info.workerDataAccessURL);
//                status = client.executeMethod(get);
//                assertEquals(HttpStatus.SC_OK, status);
//                assertEquals("foo", get.getResponseBodyAsString());
//
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(TestREST.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            utils.deleteResource(testcol, false);
//        }
//    }
//    @Test
//    public void testGetWorkersStatus() throws IOException {
//        System.err.println("testGetWorkersStatus");
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
////        rest/reservation/workers/?host=kscvdfv&host=sp2&host=192.168.1.1
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("host", "host1");
//            params.add("host", "host2");
//            params.add("host", "host3");
//
//            WebResource res = webResource.path("reservation").path("workers").queryParams(params);
//            List<WorkerStatus> list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<WorkerStatus>>() {
//            });
//
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            for (WorkerStatus w : list) {
//                assertNotNull(w.status);
//                assertNotNull(w.hostName);
//            }
//
//
//        } finally {
//            utils.deleteResource(testcol, false);
//        }
//    }
    @Test
    public void testTicketTranslator() throws IOException {
        System.err.println("testTicketTranslator");
        String testcol = root + "testResourceForTicketTranslator/";
        String testURI1 = testcol + "file1";
        try {
            utils.createCollection(testcol, true);
            utils.createFile(this.root + testURI1, true);

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
            params.add("path", "/testResourceForTicketTranslator");

            res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());

        } finally {
            utils.deleteResource(testcol, false);
        }
    }

    @Test
    public void testMetadataService() throws IOException, JAXBException {
//        if (quckTest) {
//            return;
//        }
        System.err.println("testMetadataService");
        String testcol = root + "testResourceForMetadataService/";
        String testURI1 = testcol + "file1";
        try {
            utils.deleteResource(testcol, false);
            utils.createCollection(testcol, true);
            utils.createFile(this.root + testURI1, true);
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceForMetadataService");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            LogicalDataWrapped logicalDataWrapped = null;

            Client mrClient = Client.create();

            for (LogicalDataWrapped ldw : list) {
                utils.checkLogicalDataWrapped(ldw);
                if (ldw.logicalData.type.equals("logical.file") && ldw.logicalData.name.equals("file1")) {
                    logicalDataWrapped = ldw;
                }
                params = new MultivaluedMapImpl();
                params.add("logicalExpression", "name=%22" + ldw.logicalData.name + "%22");
                params.add("logicalExpression", "description=%22LOBCDER%22");

                webResource = mrClient.resource(mrURL).path("filter").queryParams(params);

                Thread.sleep(30000);
                String response = webResource.get(String.class);
                String idStr = response.substring(response.indexOf("<localID>") + "<localID>".length(), response.indexOf("</localID>"));

                assertEquals(Integer.valueOf(ldw.logicalData.uid), Integer.valueOf(idStr));
                System.err.println(ldw.logicalData.name + ": ok");

            }
            assertNotNull(logicalDataWrapped);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestREST.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            utils.deleteResource(testcol, false);
        }
    }

    @Test
    public void testSetSpeed() throws JAXBException {
        System.err.println("testSetSpeed");
        Stats stats = new Stats();
        stats.destination = "192.168.100.5";
        stats.source = "192.168.100.1";
        stats.size = Long.valueOf(102400);
        stats.speed = 11.5;
        JAXBContext context = JAXBContext.newInstance(Stats.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        OutputStream out = new ByteArrayOutputStream();
        m.marshal(stats, out);

        WebResource webResource = restClient.resource(restURL);
        String stringStats = String.valueOf(out);

        ClientResponse response = webResource.path("lob_statistics").path("set")
                .type(MediaType.APPLICATION_XML).put(ClientResponse.class, stringStats);

        if (response.getClientResponseStatus() != ClientResponse.Status.NO_CONTENT) {
            fail();
        }
//        fail();
    }

    @Test
    public void testArchiveService() throws JAXBException, IOException, DavException, NoSuchAlgorithmException {
        System.err.println("testArchiveService");
        String testcol = root + "testResourceForArchiveService/";
        String testFileURI1 = testcol + TestSettings.TEST_FILE_NAME1;
        List<File> unzipedFiles = null;
        File randomFile = null;
        try {
            utils.deleteResource(testcol, false);
            utils.createCollection(testcol, true);
            randomFile = utils.createRandomFile("/tmp/" + TestSettings.TEST_FILE_NAME1, 1);
            //If the destination is set to this.root+testResourceId + "/file1" someone is asking for /login.html ???!!!!
            utils.postFile(randomFile, testcol);

            String localFileChecksum = utils.getChecksum(randomFile, "SHA1");
            utils.waitForReplication(testFileURI1);


            File zipFile = utils.DownloadFile(restURL + "/compress/getzip/testResourceForArchiveService", "/tmp/testResourceForArchiveService.zip", true);
            unzipedFiles = utils.unzipFile(zipFile);
            for (File f : unzipedFiles) {
                String checksumFromDownloaded = utils.getChecksum(f, "SHA1");
                assertEquals(localFileChecksum, checksumFromDownloaded);
            }



        } catch (InterruptedException ex) {
            Logger.getLogger(TestREST.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            utils.deleteResource(testcol, false);
            if (unzipedFiles != null && !unzipedFiles.isEmpty()) {
                for (File f : unzipedFiles) {
                    if (f != null) {
                        f.delete();
                    }
                }
            }

            if (randomFile != null) {
                randomFile.delete();
            }
        }
    }

    @Test
    public void testTTLService() throws JAXBException, IOException, DavException, InterruptedException {
        if (quckTest) {
            return;
        }
        System.err.println("testTTLService");
        String testcol = root + "testResourceForTTLService/";
        try {
            utils.createCollection(testcol, true);

            Long uid = utils.getResourceUID(testcol);
            WebResource webResource = restClient.resource(restURL);
            WebResource res = webResource.path("ttl").path(String.valueOf(uid)).path("3");
            ClientResponse response = res.put(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//            PUT https://lobcder.vph.cyfronet.pl/lobcder/rest/ttl/{uid}/{ttl}

            int count = 0;
            while (utils.resourceExists(testcol)) {
                count++;
                if (count > 200) {
                    fail("Resource " + testcol + " is not deleted. It should be gone");
                    break;
                }
                Thread.sleep(20000);
            }

            utils.deleteResource(testcol, false);
            utils.createCollection(testcol, true);
            webResource = restClient.resource(restURL);
            //PUT https://lobcder.vph.cyfronet.pl/lobcder/rest/ttl/{ttl}?path=/path/to/entry
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceForTTLService");
            res = webResource.path("ttl").path(String.valueOf("3")).queryParams(params);
            response = res.put(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);


            count = 0;
            while (utils.resourceExists(testcol)) {
                count++;
                if (count > 200) {
                    fail("Resource " + testcol + " is not deleted. It should be gone");
                    break;
                }
                Thread.sleep(20000);
            }

        } finally {
            utils.deleteResource(testcol, false);
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

    @XmlRootElement
    public static class Stats {

        @XmlElement(name = "source")
        String source;
        @XmlElement(name = "destination")
        String destination;
        @XmlElement(name = "size")
        Long size;
        @XmlElement(name = "speed")
        Double speed;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
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

import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

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

        restURL = prop.getProperty(("rest.test.url"), "http://localhost:8080/lobcder-2.0-SNAPSHOT/rest/");


        testcol = this.root + "testResourceId/";



        ClientConfig clientConfig = new DefaultClientConfig();
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
        assertEquals(status, HttpStatus.SC_CREATED);


        PutMethod put = new PutMethod(this.root + "testResourceId/file1");
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        status = this.client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

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
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
//            ClientResponse response = res.put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);            

            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });
            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);
            LogicalData element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertFalse(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");



        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testQueryItem() throws IOException {
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);
            LogicalData element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertFalse(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");

            //Get the uid 
            int fileUID = element.UID;
            res = webResource.path("item").path("query").path(String.valueOf(fileUID));
            LogicalData theFile = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<LogicalData>() {
            });
            assertEquals(fileUID, theFile.UID);
            assertNotNull(theFile);


            assertEquals(theFile.datatype, "logical.file");
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
            assertEquals(theFile.name, "file1");
            assertFalse(theFile.supervised);
            assertEquals(theFile.parent, "/testResourceId");
            assertEquals(theFile.contentTypesAsString, "application/octet-stream");

        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testDataItem() throws IOException {
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);
            LogicalData element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertFalse(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");

            //Get the uid 
            int fileUID = element.UID;
            res = webResource.path("item").path("data").path(String.valueOf(fileUID));
            ClientResponse response = res.get(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
            InputStream ins = response.getEntityInputStream();
            byte[] d = new byte[3];
            ins.read(d);
            ins.close();
            assertEquals(new String(d), "foo");

        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testGetSetSupervisedItems() throws IOException {

        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            //Get all unsupervised under testResourceId
            WebResource res = webResource.path("items").path("dri").path("supervised").path("false").queryParams(params);
            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);

            LogicalData element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertFalse(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");


            //Set supervised
            res = webResource.path("items").path("dri").path("supervised").path("true").queryParams(params);
            ClientResponse response = res.accept(MediaType.APPLICATION_XML).
                    put(ClientResponse.class);

            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);


            //Get all unsupervised under testResourceId
            res = webResource.path("items").path("dri").path("supervised").path("false").queryParams(params);
            list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertTrue(list.isEmpty());



            //Get all supervised under testResourceId
            res = webResource.path("items").path("dri").path("supervised").path("true").queryParams(params);
            list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);

            element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertTrue(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");

        } finally {
            deleteCollection();
        }
    }

    @Test
    public void testGetSetSupervisedItem() throws IOException {
        try {
            createCollection();
            WebResource webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            WebResource res = webResource.path("items").path("query").queryParams(params);
            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalData>>() {
            });

            assertNotNull(list);
            assertFalse(list.isEmpty());
            assertEquals(list.size(), 1);
            LogicalData element = list.get(0);
            assertEquals(element.datatype, "logical.file");
            for (Permissions p : element.permissions) {
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
            assertEquals(element.name, "file1");
            assertFalse(element.supervised);
            assertEquals(element.parent, "/testResourceId");
            assertEquals(element.contentTypesAsString, "application/octet-stream");

            //Get the uid 
            int fileUID = element.UID;
            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("supervised");

            ClientResponse response = res.get(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
            InputStream ins = response.getEntityInputStream();
            byte[] d = new byte[85];
            ins.read(d);
            ins.close();

//            System.err.println(new String(d));
            assertEquals(new String(d), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><supervised>false</supervised>");



//            - PUT http://.../rest/item/dri/{uid}/supervised/{flag}/ - sets/resets supervised  property for file with uid ID (flag = TRUE or FALSE)
            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("supervised").path(String.valueOf(Boolean.TRUE));
            response = res.put(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);



            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("supervised");
            response = res.get(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
            ins = response.getEntityInputStream();
            d = new byte[84];
            ins.read(d);
            ins.close();

            System.err.println(new String(d));
            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><supervised>true</supervised>", new String(d));

        } finally {
            deleteCollection();
        }
    }

//    @Test
//    public void testGetSetItemChecksum() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//            WebResource res = webResource.path("items").path("query").queryParams(params);
//            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            assertEquals(list.size(), 1);
//            LogicalData element = list.get(0);
//            assertEquals(element.datatype, "logical.file");
//            for (Permissions p : element.permissions) {
//                assertEquals(p.owner, username);
//                assertTrue(p.read.contains("admin"));
////                for (String s : p.read) {
////                    System.err.println("Read:" + s);
////                }
//                assertNull(p.write);
////                for (String s : p.write) {
////                    System.err.println("write:" + s);
////                }
//            }
//            assertEquals(element.name, "file1");
//            assertFalse(element.supervised);
//            assertEquals(element.parent, "/testResourceId");
//            assertEquals(element.contentTypesAsString, "application/octet-stream");
//
//            //Get the uid 
//            int fileUID = element.UID;
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("checksum");
//
//            ClientResponse response = res.get(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
//            InputStream ins = response.getEntityInputStream();
//            byte[] d = new byte[77];
//            ins.read(d);
//            ins.close();
//            assertEquals(new String(d), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><checksum>0</checksum>");
//
//
//            String checksum = "999";
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("checksum").path(checksum);
//            response = res.put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("checksum");
//            response = res.get(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
//            ins = response.getEntityInputStream();
//            d = new byte[79];
//            ins.read(d);
//            ins.close();
//            assertEquals(new String(d), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><checksum>" + checksum + "</checksum>");
//
//
//            //- GET http://.../rest/item/dri{uid}/checksum/ - returns value of checksum property  for file with uid ID
//            //- PUT http://.../rest/item/dri/{uid}/checksum/{checksum}/ sets checksum property  for file with uid ID
//
//        } finally {
//            deleteCollection();
//        }
//    }
//
//    @Test
//    public void testGetSetItemLastValidationDate() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//            WebResource res = webResource.path("items").path("query").queryParams(params);
//            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            assertEquals(list.size(), 1);
//            LogicalData element = list.get(0);
//            assertEquals(element.datatype, "logical.file");
//            for (Permissions p : element.permissions) {
//                assertEquals(p.owner, username);
//                assertTrue(p.read.contains("admin"));
////                for (String s : p.read) {
////                    System.err.println("Read:" + s);
////                }
//                assertNull(p.write);
////                for (String s : p.write) {
////                    System.err.println("write:" + s);
////                }
//            }
//            assertEquals(element.name, "file1");
//            assertFalse(element.supervised);
//            assertEquals(element.parent, "/testResourceId");
//            assertEquals(element.contentTypesAsString, "application/octet-stream");
//
//            //Get the uid 
//            int fileUID = element.UID;
//
//
////- GET http://.../rest/item/dri/{uid}/lastValidationDate/ - returns value of lastValidationDate property  for file with uid ID
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("lastValidationDate");
//
//            ClientResponse response = res.get(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
//            InputStream ins = response.getEntityInputStream();
//            byte[] d = new byte[28];
//            ins.read(d);
//            ins.close();
//            System.err.println(new String(d));
//            assertEquals(new String(d), "Thu Jan 01 01:00:00 CET 1970");
//
//
//            //- PUT http://.../rest/item/dri/ {uid}/lastValidationDate/{lastValidationDate}/ -  sets lastValidationDate property  for file with uid ID
//            String lastValidationDate = "122355000";
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("lastValidationDate").path(lastValidationDate);
//            response = res.put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//            res = webResource.path("item").path("dri").path(String.valueOf(fileUID)).path("lastValidationDate");
//            response = res.get(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK);
//            ins = response.getEntityInputStream();
//            d = new byte[28];
//            ins.read(d);
//            ins.close();
////            System.err.println(new String(d));
//            assertEquals(new String(d), "Sat Nov 17 04:30:00 CET 1973");
//
//
//            //- GET http://.../rest/item/dri{uid}/checksum/ - returns value of checksum property  for file with uid ID
//            //- PUT http://.../rest/item/dri/{uid}/checksum/{checksum}/ sets checksum property  for file with uid ID
//
//        } finally {
//            deleteCollection();
//        }
//    }
//
//    @Test
//    public void testGetSetPermissionsItems() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//            //Get permissions
//            WebResource res = webResource.path("items").path("query").queryParams(params);
//            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            assertEquals(list.size(), 1);
//            LogicalData element = list.get(0);
//            assertEquals(element.datatype, "logical.file");
//            for (Permissions p : element.permissions) {
//                assertEquals(p.owner, username);
//                assertTrue(p.read.contains("admin"));
////                for (String s : p.read) {
////                    System.err.println("Read:" + s);
////                }
//                assertNull(p.write);
////                for (String s : p.write) {
////                    System.err.println("write:" + s);
////                }
//            }
//            assertEquals(element.name, "file1");
//            assertFalse(element.supervised);
//            assertEquals(element.parent, "/testResourceId");
//            assertEquals(element.contentTypesAsString, "application/octet-stream");
//
//
//            //Set permissions
//            res = webResource.path("items").path("permissions").queryParams(params);
//            Permissions perm = new Permissions();
//            perm.owner = "aNewOwner";
//            perm.read = new HashSet<String>();
//            perm.read.add("myFriend1");
//            perm.read.add("myFriend2");
//
//            perm.write = new HashSet<String>();
//            perm.write.add("user1");
//            perm.write.add("user2");
//            ClientResponse response = res.accept(MediaType.APPLICATION_XML).put(ClientResponse.class, perm);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//
//            //Get permissions
//            res = webResource.path("items").path("query").queryParams(params);
//            list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            assertEquals(list.size(), 1);
//            element = list.get(0);
//            assertEquals(element.datatype, "logical.file");
//            for (Permissions p : element.permissions) {
//                assertEquals(p.owner, "aNewOwner");
//                assertTrue(p.read.contains("myFriend1"));
//                assertTrue(p.read.contains("myFriend2"));
////                for (String s : p.read) {
////                    System.err.println("Read:" + s);
////                }
////                for (String s : p.write) {
////                    System.err.println("write:" + s);
////                }
//                assertTrue(p.write.contains("user1"));
//                assertTrue(p.write.contains("user2"));
//            }
//            assertEquals(element.name, "file1");
//            assertFalse(element.supervised);
//            assertEquals(element.parent, "/testResourceId");
//            assertEquals(element.contentTypesAsString, "application/octet-stream");
//
//        } finally {
//            deleteCollection();
//        }
//    }
//
//    @Test
//    public void testGetSetItemPermissions() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//            WebResource res = webResource.path("items").path("query").queryParams(params);
//            List<LogicalData> list = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            assertNotNull(list);
//            assertFalse(list.isEmpty());
//            assertEquals(list.size(), 1);
//            LogicalData element = list.get(0);
//            assertEquals(element.datatype, "logical.file");
//            for (Permissions p : element.permissions) {
//                assertEquals(p.owner, username);
//                assertTrue(p.read.contains("admin"));
////                for (String s : p.read) {
////                    System.err.println("Read:" + s);
////                }
//                assertNull(p.write);
////                for (String s : p.write) {
////                    System.err.println("write:" + s);
////                }
//            }
//            assertEquals(element.name, "file1");
//            assertFalse(element.supervised);
//            assertEquals(element.parent, "/testResourceId");
//            assertEquals(element.contentTypesAsString, "application/octet-stream");
//
//            //Get the uid 
//            int fileUID = element.UID;
////            - GET http://.../rest/item/permissions/{uid}/ - returns permissions for a file (or folder) by its uid
//            res = webResource.path("item").path("permissions").path(String.valueOf(fileUID));
//            Permissions perm = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<Permissions>() {
//            });
//            assertNotNull(perm);
//            assertNull(perm.write);
//            assertNotNull(perm.read);
//            assertTrue(perm.read.contains("admin"));
//            assertEquals(perm.owner, username);
//
//            //Set permissions
////            PUT  http://.../rest/item/permissions//{uid}/ 
//            res = webResource.path("item").path("permissions").path(String.valueOf(fileUID));
//            perm = new Permissions();
//            perm.owner = "aNewOwner";
//            perm.read = new HashSet<String>();
//            perm.read.add("myFriend1");
//            perm.read.add("myFriend2");
//
//            perm.write = new HashSet<String>();
//            perm.write.add("user1");
//            perm.write.add("user2");
//            ClientResponse response = res.accept(MediaType.APPLICATION_XML).put(ClientResponse.class, perm);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//
//            res = webResource.path("item").path("permissions").path(String.valueOf(fileUID));
//            perm = res.accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<Permissions>() {
//            });
//            assertNotNull(perm);
//            assertNotNull(perm.write);
//            assertTrue(perm.write.contains("user1"));
//            assertTrue(perm.write.contains("user2"));
//
//            assertNotNull(perm.read);
//            assertTrue(perm.read.contains("myFriend1"));
//            assertTrue(perm.read.contains("myFriend2"));
//
//            assertEquals(perm.owner, "aNewOwner");
//
//        } finally {
//            deleteCollection();
//        }
//    }
//    @Test
//    public void testDRIDataResource() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//
//            //Set dir as supervised
//            ClientResponse response = webResource.path("Items").path("TRUE").
//                    queryParams(params).put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//            List<LogicalData> list = webResource.path("Items").queryParams(params).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            //Check if we get back the file in the dir 
//            assertEquals(1, list.size());
//            int uid = list.get(0).UID;
//
//            //Get the file matedata 
//            LogicalData ld = webResource.path("Item").path(String.valueOf(uid)).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<LogicalData>() {
//            });
//            //Check we got the matadata 
//            assertNotNull(ld);
//            assertEquals(uid, ld.UID);
//
//
//            //Set the file as unsupervised
//            response = webResource.path("Item").path(String.valueOf(uid)).path("supervised").path("FALSE").
//                    accept(MediaType.APPLICATION_XML).
//                    put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//            //Get the list 
//            list = webResource.path("Items").queryParams(params).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            for (LogicalData l : list) {
//                System.out.println(l.UID);
//                System.out.println(l.supervised);
//            }
//            assertFalse(list.get(0).supervised);
//
//            //Set the file as supervised
//            response = webResource.path("Item").path(String.valueOf(uid)).path("supervised").path("TRUE").
//                    accept(MediaType.APPLICATION_XML).
//                    put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//            //Get the list 
//            list = webResource.path("Items").queryParams(params).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            //Check that it's not empty 
//            assertFalse(list.isEmpty());
//            assertTrue(list.get(0).supervised);
//
//
//            //Set its checksum 
//            int checksum = 9999;
////            /{uid}/checksum/{checksum}
//            response = webResource.path("Item").path(String.valueOf(uid)).
//                    path("checksum").
//                    path(String.valueOf(checksum)).
//                    put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//            ld = webResource.path("Item").path(String.valueOf(uid)).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<LogicalData>() {
//            });
//            //Check we got the matadata 
//            assertNotNull(ld);
//            assertEquals(uid, ld.UID);
//            assertEquals(checksum, ld.checksum);
//
//
//            int validationDate = 119674740;
//            response = webResource.path("Item").path(String.valueOf(uid)).
//                    path("lastValidationDate").
//                    path(String.valueOf(validationDate)).
//                    put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//            ld = webResource.path("Item").path(String.valueOf(uid)).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<LogicalData>() {
//            });
//            //Check we got the matadata 
//            assertNotNull(ld);
//            assertEquals(uid, ld.UID);
//            assertEquals(checksum, ld.checksum);
//            assertEquals(validationDate, ld.lastValidationDate);
//
////            @Path("/{uid}//{lastValidationDate}")
//
//        } finally {
//            deleteCollection();
//        }
//    }
//
//    @Test
//    public void testDRItems() throws IOException {
//        try {
//            createCollection();
//            WebResource webResource;
//
//            webResource = restClient.resource(restURL);
//
//            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//            ClientResponse response = webResource.path("Items").path("TRUE").
//                    queryParams(params).put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//            params = new MultivaluedMapImpl();
//            params.add("path", "/testResourceId");
//
//
////            response = webResource.path("Items").
////                    accept(MediaType.APPLICATION_XML).
////                    get(ClientResponse.class);
////            String entry = response.getEntity(String.class);
////            System.out.println(entry);
//            List<LogicalData> list = webResource.path("Items").queryParams(params).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
//            assertEquals(1, list.size());
//            assertTrue(list.get(0).supervised);
//
////            for (LogicalData ld : list) {
////                System.out.println("-------------------");
////                System.out.println(ld.name);
////                System.out.println(ld.UID);
////                System.out.println(ld.checksum);
////                System.out.println(ld.contentTypesAsString);
////                System.out.println(ld.createDate);
////                System.out.println(ld.lastValidationDate);
////                System.out.println(ld.length);
////                System.out.println(ld.modifiedDate);
////                System.out.println(ld.owner);
////                System.out.println(ld.parent);
////                System.out.println("-------------------");
////            }
//
//            response = webResource.path("Items").path("FALSE").
//                    queryParams(params).put(ClientResponse.class);
//            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//
//            list = webResource.path("Items").queryParams(params).
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });
//
////            assertEquals(0, list.size());
//            assertFalse(list.get(0).supervised);
//
//        } finally {
//            deleteCollection();
//        }
//    }
    @XmlRootElement
    public static class LogicalData {

        public int checksum;
        public String contentTypes;
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
        public String datatype;
        public int UID;
        public String path;
        public Set<Permissions> permissions;
        public String parent;
    }

    @XmlRootElement
    public static class Permissions {

        public String owner;
        public Set<String> read;
        public Set<String> write;
    }
}
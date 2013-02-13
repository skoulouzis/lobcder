/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author S. Koulouzis
 */
public class TestWebWAVFS {

    private static String root;
    private static URI uri;
    private static String username1, password1;
    private static HttpClient client1;
    private static HttpClient client2;
    private static String username2;
    private static String password2;
    private static Properties prop;

    @BeforeClass
    public static void setUpClass() throws Exception {

        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder-tests"
                + File.separator + "etc" + File.separator + "test.proprties";
        prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }


        uri = URI.create(testURL);
        root = uri.toASCIIString();
        if (!root.endsWith("/")) {
            root += "/";
        }

        username1 = prop.getProperty(("webdav.test.username1"), "");
        if (username1 == null) {
            username1 = "user1";
        }
        assertTrue(username1 != null);
        password1 = prop.getProperty(("webdav.test.password1"), "");
        if (password1 == null) {
            password1 = "passwd1";
        }
        assertTrue(password1 != null);


        username2 = prop.getProperty(("webdav.test.username2"), "");
        if (username2 == null) {
            username2 = "user2";
        }
        assertTrue(username2 != null);
        password2 = prop.getProperty(("webdav.test.password2"), "");
        if (password2 == null) {
            password2 = "passwd2";
        }
        assertTrue(password2 != null);


        client1 = new HttpClient();

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client1);


        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }

        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        client1.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username1, password1));


        client2 = new HttpClient();

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client2);

        client2.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username2, password2));
    }

    @Test
    public void testCreateAndDeleteFile() throws IOException, DavException {
        //Make sure it's deleted 
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);

        testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        String testFileURI2 = uri.toASCIIString() + TestSettings.TEST_TXT_FILE_NAME;
        put = new PutMethod(testFileURI2);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        delete(testFileURI1);
        delete(testFileURI2);

    }

    /**
     * Extracts properties from a server response
     *
     * @param statusResponse
     * @return the properties
     */
    private DavPropertySet getProperties(MultiStatusResponse statusResponse) {
        Status[] status = statusResponse.getStatus();

        DavPropertySet allProp = new DavPropertySet();
        for (int i = 0; i < status.length; i++) {
            DavPropertySet pset = statusResponse.getProperties(status[i].getStatusCode());
            allProp.addAll(pset);
        }

        return allProp;
    }

    @Test
    public void testSetGetPropertySet() throws IOException, DavException {
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
        status = client1.executeMethod(propFind);
        assertEquals(HttpStatus.SC_MULTI_STATUS, status);
        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        assertEquals(HttpStatus.SC_OK, responses[0].getStatus()[0].getStatusCode());
        DavPropertySet allProp = getProperties(responses[0]);

//        DavPropertyIterator iter = allProp.iterator();
//        while (iter.hasNext()) {
//            DavProperty<?> p = iter.nextProperty();
//            System.out.println("P: " + p.getName() + " " + p.getValue());
//        }

        String isCollStr = (String) allProp.get(DavPropertyName.ISCOLLECTION).getValue();
        Boolean isCollection = Boolean.getBoolean(isCollStr);
        assertFalse(isCollection);
        String lenStr = (String) allProp.get(DavPropertyName.GETCONTENTLENGTH).getValue();
        assertEquals(Long.valueOf(lenStr), Long.valueOf(TestSettings.TEST_DATA.length()));
        String contentType = (String) allProp.get(DavPropertyName.GETCONTENTTYPE).getValue();
        //This is a bug on the milton-api. See http://jira.ettrema.com:8080/browse/MIL-119
//        assertEquals("text/plain; charset=UTF-8", contentType);
        assertTrue(contentType.contains("text"));


//        DavPropertySet newProps = new DavPropertySet();
//        DavPropertyNameSet removeProperties = new DavPropertyNameSet();
//
//        DavProperty testProp = new DefaultDavProperty("TheAnswer", "42", DavConstants.NAMESPACE);
//        newProps.add(testProp);
//        PropPatchMethod proPatch = new PropPatchMethod(testFileURI1, newProps, removeProperties);
//
//        status = client.executeMethod(proPatch);
//        assertEquals(HttpStatus.SC_MULTI_STATUS, status);
//        
//        
//        multiStatus = propFind.getResponseBodyAsMultiStatus();
//        responses = multiStatus.getResponses();
//        
//        allProp = getProperties(responses[0]);
//
//        DavPropertyIterator iter = allProp.iterator();
//        while (iter.hasNext()) {
//            DavProperty<?> p = iter.nextProperty();
//            System.out.println("P: " + p.getName() + " " + p.getValue());
//        }

        delete(testFileURI1);
    }

    @Test
    public void testPROPFIND_PUT_PROPFIND_GET_PUT() throws IOException, DavException {
        //Make sure it's deleted 
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);

        //PROPFIND file is not there 
        testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
        status = client1.executeMethod(propFind);
        assertEquals(HttpStatus.SC_NOT_FOUND, status);

        //PUT create an empty file 
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity("\n", "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        //PROPFIND get proerties 
        propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
        status = client1.executeMethod(propFind);
        assertEquals(HttpStatus.SC_MULTI_STATUS, status);


        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();

        DavPropertySet allProp = getProperties(responses[0]);
//        DavPropertyIterator iter = allProp.iterator();
//        while (iter.hasNext()) {
//            DavProperty<?> p = iter.nextProperty();
//            System.out.println("P: " + p.getName() + " " + p.getValue());
//        }

        String isCollStr = (String) allProp.get(DavPropertyName.ISCOLLECTION).getValue();
        Boolean isCollection = Boolean.getBoolean(isCollStr);
        assertFalse(isCollection);
        String lenStr = (String) allProp.get(DavPropertyName.GETCONTENTLENGTH).getValue();
        assertEquals(Long.valueOf(lenStr), Long.valueOf("\n".length()));
        String contentType = (String) allProp.get(DavPropertyName.GETCONTENTTYPE).getValue();
        //Milton bug see http://jira.ettrema.com:8080/browse/MIL-119
//        assertEquals("text/plain; charset=UTF-8", contentType);
        assertTrue(contentType.contains("text"));


        //GET the file 
        GetMethod get = new GetMethod(testFileURI1);
        status = client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals("\n", get.getResponseBodyAsString());

        //PUT
        put = new PutMethod(testFileURI1);
        String content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
        put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        get = new GetMethod(testFileURI1);
        status = client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals(content, get.getResponseBodyAsString());

        put = new PutMethod(testFileURI1);
        content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
        put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        get = new GetMethod(testFileURI1);
        status = client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals(content, get.getResponseBodyAsString());


        delete(testFileURI1);
    }

    @Test
    public void testUploadFileOnRootWithoutAdminRole() throws IOException, DavException {
        String uname = prop.getProperty(("webdav.test.non.admin.username1"), "nonAdmin");
        assertNotNull(uname);
        String pass = prop.getProperty(("webdav.test.non.admin.password1"), "secret");
        assertNotNull(pass);
        HttpClient client = new HttpClient();

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client);

        client.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(uname, pass));



        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client.executeMethod(put);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, status);
    }

    @Test
    public void testUpDownloadFileWithSpace() throws IOException, DavException {
//        String testFileURI1 = uri.toASCIIString() + "file with spaces";
//        PutMethod put = new PutMethod(testFileURI1);
//        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
//        int status = client1.executeMethod(put);
//        assertEquals(HttpStatus.SC_CREATED, status);
//
//
//        GetMethod get = new GetMethod(testFileURI1);
//        status = client1.executeMethod(get);
//        assertEquals(HttpStatus.SC_OK, status);
//        assertEquals(TestSettings.TEST_DATA, get.getResponseBodyAsString());
//
//        delete(testFileURI1);
    }
//
//    @Test
//    public void testInconsistency() {
//        try {
//            String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
//            PutMethod put = new PutMethod(testFileURI1);
//            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
//            int status = client1.executeMethod(put);
//            assertEquals(HttpStatus.SC_CREATED, status);
//
//            //Delete the physical data 
//            File f = new File(System.getProperty("user.home") + "/deleteMe/LOBCDER-REPLICA-v1.1");
//            for (File files : f.listFiles()) {
//                files.delete();
//            }
//
//            GetMethod get = new GetMethod(testFileURI1);
//            status = client1.executeMethod(get);
//            System.out.println("Status: " + status);
//
//        } catch (IOException ex) {
//            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

    @Test
    public void testMultiThread() throws IOException, DavException {
        try {
            Thread userThread1 = new UserThread(client1, uri.toASCIIString(), 1);
            userThread1.setName("T1");


            client2.getState().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    new UsernamePasswordCredentials(username1, password1));

            Thread userThread2 = new UserThread(client2, uri.toASCIIString(), 2);
            userThread2.setName("T2");

            userThread1.start();
            userThread2.start();

            userThread1.join();
            userThread2.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void delete(String testFileURI1) throws IOException {
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }

    private static class UserThread extends Thread {

        private final HttpClient client;
        private final String serverLOC;
        private final int num;

        private UserThread(HttpClient client, String serverLOC, int num) {
            this.client = client;
            this.serverLOC = serverLOC;
            this.num = num;
        }

        @Override
        public void run() {
            String testFileURI1 = serverLOC + "testFileName" + getName();
            try {
                PutMethod put = new PutMethod(testFileURI1);
                put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
                int status = client.executeMethod(put);
                assertEquals("Error wile executing PUT for " + testFileURI1, HttpStatus.SC_CREATED, status);


                PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
                status = client.executeMethod(propFind);
                assertEquals(HttpStatus.SC_MULTI_STATUS, status);
                MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
                MultiStatusResponse[] responses = multiStatus.getResponses();
                assertEquals(HttpStatus.SC_OK, responses[0].getStatus()[0].getStatusCode());

                DeleteMethod del = new DeleteMethod(testFileURI1);
                status = client.executeMethod(del);
                assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

            } catch (DavException ex) {
                Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

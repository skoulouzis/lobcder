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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.security.Principal;
import org.apache.jackrabbit.webdav.security.Privilege;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author S. Koulouzis
 */
public class TestWebWAVFS {

    private String root;
    private URI uri;
    private String username1, password1;
    private HttpClient client1;
    private HttpClient client2;
    private String username2;
    private String password2;

    @Before
    public void setUp() throws Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder-tests"
                + File.separator + "etc" + File.separator + "test.proprties";
        Properties prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
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

        this.username1 = prop.getProperty(("webdav.test.username1"), "");
        if (username1 == null) {
            username1 = "user1";
        }
        assertTrue(username1 != null);
        this.password1 = prop.getProperty(("webdav.test.password1"), "");
        if (password1 == null) {
            password1 = "passwd1";
        }
        assertTrue(password1 != null);


        this.username2 = prop.getProperty(("webdav.test.username2"), "");
        if (username2 == null) {
            username2 = "user2";
        }
        assertTrue(username2 != null);
        this.password2 = prop.getProperty(("webdav.test.password2"), "");
        if (password2 == null) {
            password2 = "passwd2";
        }
        assertTrue(password2 != null);


        this.client1 = new HttpClient();

        assertNotNull(this.uri.getHost());
        assertNotNull(this.uri.getPort());
        assertNotNull(this.client1);

        this.client1.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username1, this.password1));


        this.client2 = new HttpClient();

        assertNotNull(this.uri.getHost());
        assertNotNull(this.uri.getPort());
        assertNotNull(this.client2);

        this.client2.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username2, this.password2));
    }

    @Test
    public void testCreateAndDeleteFile() throws IOException, DavException {
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        String testFileURI2 = this.uri.toASCIIString() + TestSettings.TEST_TXT_FILE_NAME;
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
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1+".txt";
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
    public void testSetGetACL() throws IOException, DavException {

        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        Principal principal = Principal.getAllPrincipal();

        Privilege[] privileges = new Privilege[1];
        privileges[0] = Privilege.PRIVILEGE_ALL;
        boolean invert = false;
        boolean isProtected = false;


//        AclResource inheritedFrom = null;
//        Ace ace = AclProperty.createGrantAce(principal, privileges, invert, isProtected, inheritedFrom);
//        Ace[] accessControlElements = new Ace[1];
//        accessControlElements[0] = ace;
//        AclProperty aclProp = new AclProperty(accessControlElements);

        //       org.apache.jackrabbit.webdav.client.methods.AclMethod acl = new AclMethod(testFileURI1, new AclProperty(accessControlElements)); 


        delete(testFileURI1);


    }

    @Test
    public void testPROPFIND_PUT_PROPFIND_GET_PUT() throws IOException, DavException {

        //PROPFIND file is not there 
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1+".txt";
        PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
        int status = client1.executeMethod(propFind);
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
        status = this.client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals("\n", get.getResponseBodyAsString());

        //PUT
        put = new PutMethod(testFileURI1);
        String content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
        put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        get = new GetMethod(testFileURI1);
        status = this.client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals(content, get.getResponseBodyAsString());

        put = new PutMethod(testFileURI1);
        content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
        put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        get = new GetMethod(testFileURI1);
        status = this.client1.executeMethod(get);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals(content, get.getResponseBodyAsString());


        delete(testFileURI1);
    }

    @Test
    public void testMultiThread() throws IOException, DavException {
        try {
            Thread userThread1 = new UserThread(this.client1, this.uri.toASCIIString(), 1);
            userThread1.setName("T1");
            Thread userThread2 = new UserThread(this.client2, this.uri.toASCIIString(), 2);
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
            String testFileURI1 = serverLOC + "/testFileName" + getName();
            try {
                PutMethod put = new PutMethod(testFileURI1);
                put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
                int status = client.executeMethod(put);
                assertEquals(HttpStatus.SC_CREATED, status);


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

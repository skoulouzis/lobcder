/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.security.AclProperty.Ace;
import org.apache.jackrabbit.webdav.security.AclResource;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.security.Principal;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.Status;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import java.io.IOException;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import java.io.File;
import java.net.URI;
import java.util.Properties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.AclMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.security.AclProperty;
import org.junit.Before;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author S. Koulouzis
 */
public class TestWebWAVFS {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;

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

        this.uri = URI.create(testURL);
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }

        this.username = prop.getProperty(("webdav.test.username"), "");
        if (username == null) {
            username = "user";
        }
        assertTrue(username != null);
        this.password = prop.getProperty(("webdav.test.password"), "");
        if (password == null) {
            password = "passwd";
        }
        assertTrue(password != null);

        this.client = new HttpClient();

        assertNotNull(this.uri.getHost());
        assertNotNull(this.uri.getPort());
        assertNotNull(this.client);

        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));
    }

    @Test
    public void testCreateAndDeleteFile() throws IOException, DavException {
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        
        String testFileURI2 = this.uri.toASCIIString() + TestSettings.TEST_TXT_FILE_NAME;
        put = new PutMethod(testFileURI2);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        status = client.executeMethod(put);
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
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME;
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
        assertEquals("text/plain; charset=UTF-8", contentType);




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

        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        Principal principal = Principal.getAllPrincipal();
        Privilege[] privileges = new Privilege[1];
        privileges[0] = Privilege.PRIVILEGE_ALL;
        boolean invert = false;
        boolean isProtected = false;
        AclResource inheritedFrom = null;// new DavResourceImpl();
        Ace ace = AclProperty.createGrantAce(principal, privileges, invert,
                isProtected, inheritedFrom);
        Ace[] accessControlElements = new Ace[1];
        accessControlElements[0] = ace;
        AclProperty aclProp = new AclProperty(accessControlElements);

        //       org.apache.jackrabbit.webdav.client.methods.AclMethod acl = new AclMethod(testFileURI1, new AclProperty(accessControlElements)); 


        delete(testFileURI1);


    }

    private void delete(String testFileURI1) throws IOException {
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.client.methods.AclMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.security.*;
import org.apache.jackrabbit.webdav.security.AclProperty.Ace;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author S. koulouzis
 *
 */
public class WebDAVSecurityTest {

    private static String root;
    private static URI uri;
    private static String username1, password1;
    private static HttpClient client1;
    private static HttpClient client2;
    private static String username2;
    private static String password2;

    @BeforeClass
    public static void setUpClass() throws Exception {

        String propBasePath = "etc" + File.separator + "test.properties";
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


//         acMgr = getAccessControlManager(superuser);
    }

//    @Test
//    public void testGetCurrentUserPrivilegeSet() throws UnsupportedEncodingException, IOException, DavException {
//        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
//        try {
//
//            DeleteMethod delete = new DeleteMethod(testFileURI1);
//            int status = client1.executeMethod(delete);
//
//
//            PutMethod put = new PutMethod(testFileURI1);
//            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
//            status = client1.executeMethod(put);
//            assertEquals(HttpStatus.SC_CREATED, status);
//
//            DavPropertyNameSet d = new DavPropertyNameSet();
//            DavPropertyName userPriv = DavPropertyName.create("current-user-privilege-set");
//            d.add(userPriv);
//
//            PropFindMethod propFind = new PropFindMethod(testFileURI1, d, DavConstants.DEPTH_INFINITY);
//            status = client1.executeMethod(propFind);
//            assertEquals(HttpStatus.SC_MULTI_STATUS, status);
//
//
//            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
//            MultiStatusResponse[] responses = multiStatus.getResponses();
//
//            for (MultiStatusResponse r : responses) {
//                System.out.println("Responce: " + r.getHref());
//                DavPropertySet allProp = getProperties(r);
//
//                DavPropertyIterator iter = allProp.iterator();
//                while (iter.hasNext()) {
//                    DavProperty<?> p = iter.nextProperty();
//                    System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
//                }
//            }
//
//            assertEquals(HttpStatus.SC_OK, responses[0].getStatus()[0].getStatusCode());
//            DavPropertySet allProp = getProperties(responses[0]);
//            DavProperty<?> prop = allProp.get(userPriv);
//            assertEquals(userPriv, prop.getName());
//
////             ArrayList<org.apache.xerces.dom.DeferredElementNSImpl> value =  (ArrayList<org.apache.xerces.dom.DeferredElementNSImpl>) prop.getValue();
////             for(org.apache.xerces.dom.DeferredElementNSImpl el : value){
////                 System.out.println("DeferredElementNSImpl: "+el.getBaseURI());
////                 System.out.println("getLocalName: "+el.getLocalName());
////                 System.out.println("getNodeValue: "+el.getNodeValue());
////             }
////            String value = (String) prop.getValue();
////            assertTrue(value.contains("READ") || value.contains("ALL"));
//            System.out.println("Name: " + prop.getName() + " Values " + prop.getValue() + " class: " + prop.getValue().getClass().getName() + " " + prop.getValue().getClass().getName());
//        } finally {
//            DeleteMethod delete = new DeleteMethod(testFileURI1);
//            int status = client1.executeMethod(delete);
//            assertTrue("DeleteMethod status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
//        }
//    }
//
//    @Test
//    public void testUnothorizedCreateFile() {
//        try {
//            String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
//            PutMethod put = new PutMethod(testFileURI1);
//            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
//
//            client2.getState().setCredentials(
//                    new AuthScope(uri.getHost(), uri.getPort()),
//                    new UsernamePasswordCredentials(username2, "WRONG_PASSWORD"));
//
//            int status = client2.executeMethod(put);
//            assertEquals(HttpStatus.SC_UNAUTHORIZED, status);
//
//        } catch (IOException ex) {
//            Logger.getLogger(WebDAVSecurityTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    @Test
//    public void testGetSetACL() throws IOException, DavException {
//        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
//        DeleteMethod del = new DeleteMethod(testFileURI1);
//        client1.executeMethod(del);
//
//        PutMethod put = new PutMethod(testFileURI1);
//        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
//        int status = client1.executeMethod(put);
//        assertEquals(HttpStatus.SC_CREATED, status);
//
//        DavPropertyNameSet d = new DavPropertyNameSet();
//        d.add(SecurityConstants.ACL);
//        PropFindMethod propFind = new PropFindMethod(testFileURI1, d, DavConstants.DEPTH_INFINITY);
//
//        status = client1.executeMethod(propFind);
//        assertEquals(HttpStatus.SC_MULTI_STATUS, status);
//
//
//        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
//        MultiStatusResponse[] responses = multiStatus.getResponses();
//
//        for (MultiStatusResponse r : responses) {
//            System.out.println("Responce: " + r.getHref());
//            DavPropertySet allProp = getProperties(r);
//
//            DavPropertyIterator iter = allProp.iterator();
//            while (iter.hasNext()) {
//                DavProperty<?> p = iter.nextProperty();
////                assertNotNull(p.getValue());
//                System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
//            }
//
//        }
//    }

    @Test
    public void testAclMethod() throws IOException, DavException {
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        DeleteMethod del = new DeleteMethod(testFileURI1);
        client1.executeMethod(del);

        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        Privilege[] privs = new Privilege[]{
            Privilege.PRIVILEGE_WRITE,
            Privilege.PRIVILEGE_READ, ///Privilege.getPrivilege("schedule-query-freebusy", SecurityConstants.NAMESPACE)
        };

        Principal principal = Principal.getHrefPrincipal(username1);

        Ace ace1 = AclProperty.createGrantAce(principal,
                privs, false, false, null);
        
        

        Principal principal2 = Principal.getHrefPrincipal("someUser");

        Ace ace2 = AclProperty.createDenyAce(principal2, privs, true, true, null);

        AclMethod acl = new AclMethod(testFileURI1, new AclProperty(new Ace[]{ace1,ace2}));
        Header[] headers = acl.getRequestHeaders();
        for (Header h : headers) {
            System.out.println("getRequestHeaders: " + h.getName() + " : " + h.getValue());
        }

        status = client1.executeMethod(acl);
        assertEquals(HttpStatus.SC_OK, status);


//        DavPropertyNameSet d = new DavPropertyNameSet();
//        d.add(SecurityConstants.ACL);
////        d.add(SecurityConstants.ACL_RESTRICTIONS);
////        d.add(SecurityConstants.ALTERNATE_URI_SET);
////        d.add(SecurityConstants.CURRENT_USER_PRIVILEGE_SET);
////        d.add(SecurityConstants.GROUP);
////        d.add(SecurityConstants.GROUP_MEMBERSHIP);
////        d.add(SecurityConstants.GROUP_MEMBER_SET);
////        d.add(SecurityConstants.INHERITED_ACL_SET);
////        d.add(SecurityConstants.OWNER);
////        d.add(SecurityConstants.PRINCIPAL_COLLECTION_SET);
////        d.add(SecurityConstants.PRINCIPAL_URL);
////        d.add(SecurityConstants.SUPPORTED_PRIVILEGE_SET);
//        PropFindMethod propFind = new PropFindMethod(testFileURI1, d, DavConstants.DEPTH_INFINITY);
//
//        status = client1.executeMethod(propFind);
//        assertEquals(HttpStatus.SC_MULTI_STATUS, status);
//
//
//        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
//        MultiStatusResponse[] responses = multiStatus.getResponses();
//
//        for (MultiStatusResponse r : responses) {
//            System.out.println("Responce: " + r.getHref());
//            DavPropertySet allProp = getProperties(r);
//
//            DavPropertyIterator iter = allProp.iterator();
//            while (iter.hasNext()) {
//                DavProperty<?> p = iter.nextProperty();
//                assertNotNull(p.getValue());
//                System.out.println("\tName: " + p.getName() + " Values " + p.getValue());
//            }
//        }
    }
}

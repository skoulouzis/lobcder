///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package nl.uva.cs.lobcder.webDav.resources;
//
//import nl.uva.cs.lobcder.util.Constants;
//import com.bradmcevoy.common.Path;
//import com.bradmcevoy.http.Auth;
//import com.bradmcevoy.http.CollectionResource;
//import com.bradmcevoy.http.MiltonServlet;
//import com.bradmcevoy.http.Range;
//import java.io.*;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.servlet.http.HttpServletRequest;
//import nl.uva.cs.lobcder.auth.Permissions;
//import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
//import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
//import nl.uva.cs.lobcder.frontend.WebDavServlet;
//import nl.uva.cs.lobcder.resources.*;
//import nl.uva.cs.lobcder.util.ConstantsAndSettings;
//import nl.uva.cs.lobcder.util.DummyHttpServletRequest;
//import static org.junit.Assert.*;
//import org.junit.*;
//
///**
// *
// * @author S. koulouzis
// */
//public class WebDataDirResourceTest {
//
//    private RDMSDLCatalog catalogue;
////    private LogicalData testLogicalData;
//    private Path testFolderPath;
//    private StorageSite site;
//    private ArrayList<IStorageSite> sites;
//    private LogicalData testLogicalFile;
//    private LogicalData testLogicalFolder;
//
//    public WebDataDirResourceTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//    }
//
//    @Before
//    public void setUp() {
//        try {
//            String confDir = nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR;
//            File propFile = new File(confDir + "/datanucleus.properties");
//            catalogue = new RDMSDLCatalog(propFile);
//
//
//            testLogicalFile = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_1, Constants.LOGICAL_FILE);
//
//            testFolderPath = Path.path("/WebDataDirResourceTestCollection1");
//            testLogicalFolder = new LogicalData(testFolderPath, Constants.LOGICAL_FOLDER);
//
//            String endpoint = "file:///tmp/";
//            String vphUser = "user1";
//            Credential cred = new Credential(vphUser.split(","));
//            site = new StorageSite(endpoint, cred);
//
//            sites = new ArrayList<IStorageSite>();
//            sites.add(site);
//        } catch (Exception ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    @After
//    public void tearDown() {
//    }
//
////    /**
////     * Test of createCollection method, of class WebDataDirResource.
////     */
////    @Test
////    public void testCreateCollection() throws Exception {
////        System.out.println("createCollection");
////        String newName = "";
////        
////        catalogue.registerResourceEntry(testLogicalData);
////        WebDataDirResource instance = new WebDataDirResource(catalogue, testLogicalData);
////        
////        CollectionResource expResult = null;
////        CollectionResource result = instance.createCollection(newName);
//////        assertEquals(expResult, result);
////        
////    }
////    /**
////     * Test of child method, of class WebDataDirResource.
////     */
////    @Test
////    public void testChild() {
////        System.out.println("child");
////        String childName = "";
////        WebDataDirResource instance = null;
////        Resource expResult = null;
////        Resource result = instance.child(childName);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getChildren method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetChildren() {
////        System.out.println("getChildren");
////        WebDataDirResource instance = null;
////        List expResult = null;
////        List result = instance.getChildren();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getUniqueId method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetUniqueId() {
////        System.out.println("getUniqueId");
////        WebDataDirResource instance = null;
////        String expResult = "";
////        String result = instance.getUniqueId();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getName method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetName() {
////        System.out.println("getName");
////        WebDataDirResource instance = null;
////        String expResult = "";
////        String result = instance.getName();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of authenticate method, of class WebDataDirResource.
////     */
////    @Test
////    public void testAuthenticate() {
////        System.out.println("authenticate");
////        String user = "";
////        String password = "";
////        WebDataDirResource instance = null;
////        Object expResult = null;
////        Object result = instance.authenticate(user, password);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of authorise method, of class WebDataDirResource.
////     */
////    @Test
////    public void testAuthorise() {
////        System.out.println("authorise");
////        Request request = null;
////        Method method = null;
////        Auth auth = null;
////        WebDataDirResource instance = null;
////        boolean expResult = false;
////        boolean result = instance.authorise(request, method, auth);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getRealm method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetRealm() {
////        System.out.println("getRealm");
////        WebDataDirResource instance = null;
////        String expResult = "";
////        String result = instance.getRealm();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getModifiedDate method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetModifiedDate() {
////        System.out.println("getModifiedDate");
////        WebDataDirResource instance = null;
////        Date expResult = null;
////        Date result = instance.getModifiedDate();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of checkRedirect method, of class WebDataDirResource.
////     */
////    @Test
////    public void testCheckRedirect() {
////        System.out.println("checkRedirect");
////        Request request = null;
////        WebDataDirResource instance = null;
////        String expResult = "";
////        String result = instance.checkRedirect(request);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
//    /**
//     * Test of createNew method, of class WebDataDirResource.
//     */
//    @Test
//    public void testCreateNew() throws Exception {
//        System.out.println("createNew");
//
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//
//        testLogicalFolder.setStorageSites(sites);
//        catalogue.registerResourceEntry(testLogicalFolder);
//
//
//        ILogicalData loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//        WebDataDirResource instance = createDirResource(catalogue, loaded);
//        WebDataFileResource result = (WebDataFileResource) instance.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        assertNotNull(result);
//        assertEquals(new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), result.getContentLength());
//        loaded = catalogue.getResourceEntryByLDRI(Path.path(testLogicalFolder.getLDRI(), ConstantsAndSettings.TEST_FILE_NAME_1));
//        assertNotNull(loaded);
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        Range range = null;
//        Map<String, String> params = null;
//        String contentType = "text/plain";
//        result.sendContent(out, range, params, contentType);
//        String content = new String(out.toByteArray());
//        assertEquals(ConstantsAndSettings.TEST_DATA, content);
//
//
//        loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//        instance = new WebDataDirResource(catalogue, loaded);
//        instance.delete();
//
//        loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//        assertNull(loaded);
//
//        loaded = catalogue.getResourceEntryByLDRI(Path.path(testLogicalFolder.getLDRI(), ConstantsAndSettings.TEST_FILE_NAME_1));
//        assertNull(loaded);
//    }
//
////    /**
////     * Test of copyTo method, of class WebDataDirResource.
////     */
////    @Test
////    public void testCopyTo() throws Exception {
////        System.out.println("copyTo");
////        CollectionResource toCollection = null;
////        String name = "";
////        WebDataDirResource instance = null;
////        instance.copyTo(toCollection, name);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of delete method, of class WebDataDirResource.
////     */
////    @Test
////    public void testDelete() throws Exception {
////        System.out.println("delete");
////        WebDataDirResource instance = null;
////        instance.delete();
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of sendContent method, of class WebDataDirResource.
////     */
////    @Test
////    public void testSendContent() throws Exception {
////        System.out.println("sendContent");
////        OutputStream out = null;
////        Range range = null;
////        Map<String, String> params = null;
////        String contentType = "";
////        WebDataDirResource instance = null;
////        instance.sendContent(out, range, params, contentType);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
//
////    /**
////     * Test of getMaxAgeSeconds method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetMaxAgeSeconds() {
////        System.out.println("getMaxAgeSeconds");
////        Auth auth = null;
////        WebDataDirResource instance = null;
////        Long expResult = null;
////        Long result = instance.getMaxAgeSeconds(auth);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getContentType method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetContentType() {
////        System.out.println("getContentType");
////        String accepts = "";
////        WebDataDirResource instance = null;
////        String expResult = "";
////        String result = instance.getContentType(accepts);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getContentLength method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetContentLength() {
////        System.out.println("getContentLength");
////        WebDataDirResource instance = null;
////        Long expResult = null;
////        Long result = instance.getContentLength();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of moveTo method, of class WebDataDirResource.
////     */
////    @Test
////    public void testMoveTo() throws Exception {
////        System.out.println("moveTo");
////        CollectionResource rDest = null;
////        String name = "";
////        WebDataDirResource instance = null;
////        instance.moveTo(rDest, name);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
//
////    /**
////     * Test of getCreateDate method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetCreateDate() {
////        System.out.println("getCreateDate");
////        WebDataDirResource instance = null;
////        Date expResult = null;
////        Date result = instance.getCreateDate();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of debug method, of class WebDataDirResource.
////     */
////    @Test
////    public void testDebug() {
////        System.out.println("debug");
////        String msg = "";
////        WebDataDirResource instance = null;
////        instance.debug(msg);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getPath method, of class WebDataDirResource.
////     */
////    @Test
////    public void testGetPath() {
////        System.out.println("getPath");
////        WebDataDirResource instance = null;
////        Path expResult = null;
////        Path result = instance.getPath();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
//
//    private WebDataDirResource createDirResource(IDLCatalogue catalogue, ILogicalData logicalData) throws IOException, Exception {
//        ArrayList<Integer> permArr = new ArrayList<Integer>();
//        permArr.add(0);
//        permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
//        permArr.add(Permissions.REST_ROLE | Permissions.NOACCESS);
//        permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
//        Metadata meta =logicalData.getMetadata();
//        meta.setPermissionArray(permArr);
//        logicalData.setMetadata(meta);
//        catalogue.updateResourceEntry(logicalData);
//        
//        WebDataDirResource instance = new WebDataDirResource(catalogue, logicalData);
//
//        HttpServletRequest r = new DummyHttpServletRequest();
//        WebDavServlet.setThreadlocals(r, null);
//        instance.authenticate("user", "pass");
//        return instance;
//    }
//}

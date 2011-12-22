/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import java.util.logging.Level;
import java.util.ArrayList;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import java.util.logging.Logger;
import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author S. koulouzis
 */
public class WebDataDirResourceTest {
    private SimpleDLCatalogue catalogue;
    private String testFileName;
    private Path testFilePath;
    private LogicalFile testLogicalFile;
    private String testFolderName;
    private Path testFolderPath;
    private LogicalFolder testLogicalFolder;
    private StorageSite site;
    private ArrayList<StorageSite> sites;
    private String testData;

    public WebDataDirResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try {
            catalogue = new SimpleDLCatalogue();

            testFileName = "testFile";
            testFilePath = Path.path(testFileName);
            testLogicalFile = new LogicalFile(testFilePath);

            testFolderName = "testFolder";
            testFolderPath = Path.path(testFolderName);
            testLogicalFolder = new LogicalFolder(testFolderPath);

            String endpoint = "file:///tmp/";
            String vphUser = "user1";
            Credential cred = new Credential(vphUser);
            site = new StorageSite(endpoint, cred);

            sites = new ArrayList<StorageSite>();
            sites.add(site);

            testData = "Tell me, O muse, of that ingenious hero who travelled "
                    + "far and wide after he had sacked the famous town of Troy. "
                    + "Many cities did he visit, and many were the nations with "
                    + "whose manners and customs he was acquainted; moreover he "
                    + "suffered much by sea while trying to save his own life "
                    + "and bring his men safely home; but do what he might he "
                    + "could not save his men, for they perished through their "
                    + "own sheer folly in eating the cattle of the Sun-god "
                    + "Hyperion; so the god prevented them from ever reaching "
                    + "home.";
        } catch (Exception ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @After
    public void tearDown() {
    }

//    /**
//     * Test of createCollection method, of class WebDataDirResource.
//     */
//    @Test
//    public void testCreateCollection() throws Exception {
//        System.out.println("createCollection");
//        String newName = "";
//        
//        catalogue.registerResourceEntry(testLogicalFolder);
//        WebDataDirResource instance = new WebDataDirResource(catalogue, testLogicalFolder);
//        
//        CollectionResource expResult = null;
//        CollectionResource result = instance.createCollection(newName);
////        assertEquals(expResult, result);
//        
//    }

//    /**
//     * Test of child method, of class WebDataDirResource.
//     */
//    @Test
//    public void testChild() {
//        System.out.println("child");
//        String childName = "";
//        WebDataDirResource instance = null;
//        Resource expResult = null;
//        Resource result = instance.child(childName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getChildren method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetChildren() {
//        System.out.println("getChildren");
//        WebDataDirResource instance = null;
//        List expResult = null;
//        List result = instance.getChildren();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getUniqueId method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetUniqueId() {
//        System.out.println("getUniqueId");
//        WebDataDirResource instance = null;
//        String expResult = "";
//        String result = instance.getUniqueId();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getName method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetName() {
//        System.out.println("getName");
//        WebDataDirResource instance = null;
//        String expResult = "";
//        String result = instance.getName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of authenticate method, of class WebDataDirResource.
//     */
//    @Test
//    public void testAuthenticate() {
//        System.out.println("authenticate");
//        String user = "";
//        String password = "";
//        WebDataDirResource instance = null;
//        Object expResult = null;
//        Object result = instance.authenticate(user, password);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of authorise method, of class WebDataDirResource.
//     */
//    @Test
//    public void testAuthorise() {
//        System.out.println("authorise");
//        Request request = null;
//        Method method = null;
//        Auth auth = null;
//        WebDataDirResource instance = null;
//        boolean expResult = false;
//        boolean result = instance.authorise(request, method, auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRealm method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetRealm() {
//        System.out.println("getRealm");
//        WebDataDirResource instance = null;
//        String expResult = "";
//        String result = instance.getRealm();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getModifiedDate method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetModifiedDate() {
//        System.out.println("getModifiedDate");
//        WebDataDirResource instance = null;
//        Date expResult = null;
//        Date result = instance.getModifiedDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkRedirect method, of class WebDataDirResource.
//     */
//    @Test
//    public void testCheckRedirect() {
//        System.out.println("checkRedirect");
//        Request request = null;
//        WebDataDirResource instance = null;
//        String expResult = "";
//        String result = instance.checkRedirect(request);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of createNew method, of class WebDataDirResource.
//     */
//    @Test
//    public void testCreateNew() throws Exception {
//        System.out.println("createNew");
//        String newName = "";
//        InputStream inputStream = null;
//        Long length = null;
//        String contentType = "";
//        WebDataDirResource instance = null;
//        Resource expResult = null;
//        Resource result = instance.createNew(newName, inputStream, length, contentType);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of copyTo method, of class WebDataDirResource.
//     */
//    @Test
//    public void testCopyTo() throws Exception {
//        System.out.println("copyTo");
//        CollectionResource toCollection = null;
//        String name = "";
//        WebDataDirResource instance = null;
//        instance.copyTo(toCollection, name);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of delete method, of class WebDataDirResource.
//     */
//    @Test
//    public void testDelete() throws Exception {
//        System.out.println("delete");
//        WebDataDirResource instance = null;
//        instance.delete();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sendContent method, of class WebDataDirResource.
//     */
//    @Test
//    public void testSendContent() throws Exception {
//        System.out.println("sendContent");
//        OutputStream out = null;
//        Range range = null;
//        Map<String, String> params = null;
//        String contentType = "";
//        WebDataDirResource instance = null;
//        instance.sendContent(out, range, params, contentType);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMaxAgeSeconds method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetMaxAgeSeconds() {
//        System.out.println("getMaxAgeSeconds");
//        Auth auth = null;
//        WebDataDirResource instance = null;
//        Long expResult = null;
//        Long result = instance.getMaxAgeSeconds(auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getContentType method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetContentType() {
//        System.out.println("getContentType");
//        String accepts = "";
//        WebDataDirResource instance = null;
//        String expResult = "";
//        String result = instance.getContentType(accepts);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getContentLength method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetContentLength() {
//        System.out.println("getContentLength");
//        WebDataDirResource instance = null;
//        Long expResult = null;
//        Long result = instance.getContentLength();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of moveTo method, of class WebDataDirResource.
//     */
//    @Test
//    public void testMoveTo() throws Exception {
//        System.out.println("moveTo");
//        CollectionResource rDest = null;
//        String name = "";
//        WebDataDirResource instance = null;
//        instance.moveTo(rDest, name);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCreateDate method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetCreateDate() {
//        System.out.println("getCreateDate");
//        WebDataDirResource instance = null;
//        Date expResult = null;
//        Date result = instance.getCreateDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of debug method, of class WebDataDirResource.
//     */
//    @Test
//    public void testDebug() {
//        System.out.println("debug");
//        String msg = "";
//        WebDataDirResource instance = null;
//        instance.debug(msg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getPath method, of class WebDataDirResource.
//     */
//    @Test
//    public void testGetPath() {
//        System.out.println("getPath");
//        WebDataDirResource instance = null;
//        Path expResult = null;
//        Path result = instance.getPath();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}

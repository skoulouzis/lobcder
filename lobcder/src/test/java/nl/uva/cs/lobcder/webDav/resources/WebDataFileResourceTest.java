/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import com.bradmcevoy.common.Path;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author skoulouz
 */
public class WebDataFileResourceTest {

    private SimpleDLCatalogue catalogue;

    public WebDataFileResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        catalogue = new SimpleDLCatalogue();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of copyTo method, of class WebDataFileResource.
     */
    @Test
    public void testCopyTo() {
        System.out.println("copyTo");
        WebDataDirResource collectionResource = null;
        try {
            String testColl = "testCopyToColl";
            Path testCollPath = Path.path(testColl);
            ILogicalData testLogicalFolder = new LogicalFolder(testCollPath);
            
//            catalogue.registerResourceEntry(testLogicalFolder);
            collectionResource = new WebDataDirResource(catalogue, testLogicalFolder);

            String testFile = "testCopyToFile";
            Path testFilePath = Path.path(testFile);
            ILogicalData testLogicalFile = new LogicalFile(testFilePath);

            WebDataFileResource instance = new WebDataFileResource(catalogue, testLogicalFile);
            instance.copyTo(collectionResource, instance.getName());

            List<? extends Resource> children = collectionResource.getChildren();
            for (Resource r : children) {
                System.out.println("Ch: " + r.getName());
            }
            // TODO review the generated test code and remove the default call to fail.
//            fail("The test case is a prototype.");
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                collectionResource.delete();
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
//    /**
//     * Test of delete method, of class WebDataFileResource.
//     */
//    @Test
//    public void testDelete() {
//        System.out.println("delete");
//        WebDataFileResource instance = null;
//        instance.delete();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getContentLength method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetContentLength() {
//        System.out.println("getContentLength");
//        WebDataFileResource instance = null;
//        Long expResult = null;
//        Long result = instance.getContentLength();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getContentType method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetContentType() {
//        System.out.println("getContentType");
//        String accepts = "";
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.getContentType(accepts);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMaxAgeSeconds method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetMaxAgeSeconds() {
//        System.out.println("getMaxAgeSeconds");
//        Auth auth = null;
//        WebDataFileResource instance = null;
//        Long expResult = null;
//        Long result = instance.getMaxAgeSeconds(auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sendContent method, of class WebDataFileResource.
//     */
//    @Test
//    public void testSendContent() throws Exception {
//        System.out.println("sendContent");
//        OutputStream out = null;
//        Range range = null;
//        Map<String, String> params = null;
//        String contentType = "";
//        WebDataFileResource instance = null;
//        instance.sendContent(out, range, params, contentType);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of moveTo method, of class WebDataFileResource.
//     */
//    @Test
//    public void testMoveTo() throws Exception {
//        System.out.println("moveTo");
//        CollectionResource rDest = null;
//        String name = "";
//        WebDataFileResource instance = null;
//        instance.moveTo(rDest, name);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of processForm method, of class WebDataFileResource.
//     */
//    @Test
//    public void testProcessForm() throws Exception {
//        System.out.println("processForm");
//        Map<String, String> arg0 = null;
//        Map<String, FileItem> arg1 = null;
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.processForm(arg0, arg1);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of debug method, of class WebDataFileResource.
//     */
//    @Test
//    public void testDebug() {
//        System.out.println("debug");
//        String msg = "";
//        WebDataFileResource instance = null;
//        instance.debug(msg);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getUniqueId method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetUniqueId() {
//        System.out.println("getUniqueId");
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.getUniqueId();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getName method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetName() {
//        System.out.println("getName");
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.getName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of authenticate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testAuthenticate() {
//        System.out.println("authenticate");
//        String user = "";
//        String password = "";
//        WebDataFileResource instance = null;
//        Object expResult = null;
//        Object result = instance.authenticate(user, password);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of authorise method, of class WebDataFileResource.
//     */
//    @Test
//    public void testAuthorise() {
//        System.out.println("authorise");
//        Request request = null;
//        Method method = null;
//        Auth auth = null;
//        WebDataFileResource instance = null;
//        boolean expResult = false;
//        boolean result = instance.authorise(request, method, auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRealm method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetRealm() {
//        System.out.println("getRealm");
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.getRealm();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getModifiedDate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetModifiedDate() {
//        System.out.println("getModifiedDate");
//        WebDataFileResource instance = null;
//        Date expResult = null;
//        Date result = instance.getModifiedDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of checkRedirect method, of class WebDataFileResource.
//     */
//    @Test
//    public void testCheckRedirect() {
//        System.out.println("checkRedirect");
//        Request request = null;
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.checkRedirect(request);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCreateDate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetCreateDate() {
//        System.out.println("getCreateDate");
//        WebDataFileResource instance = null;
//        Date expResult = null;
//        Date result = instance.getCreateDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}

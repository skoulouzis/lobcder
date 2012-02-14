/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import java.util.Map;
import com.bradmcevoy.http.Range;
import java.io.ByteArrayOutputStream;
import com.bradmcevoy.http.Resource;
import java.util.List;
import nl.uva.cs.lobcder.resources.IStorageSite;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import java.io.ByteArrayInputStream;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResourceFactoryTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        //clean up 
        String host = "localhost:8080";
        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        if (result != null) {
            result.delete();
        }
        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_2);
        if (result != null) {
            result.delete();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

//    /**
//     * Test of getResource method, of class WebDataResourceFactory.
//     */
//    @Test
//    public void testGetResource() throws Exception {
//        System.out.println("getResource");
//        String host = "localhost:8080";
//        String strPath = "/";
//        WebDataResourceFactory instance = new WebDataResourceFactory();
////        Resource expResult = null;
//        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(result, file);
//
//        result.delete();
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//
//    }
//
//    /**
//     * Test of getResource method, of class WebDataResourceFactory.
//     */
//    @Test
//    public void testCreateGetAndDeleteResource() throws Exception {
//        System.out.println("getResource");
//        String host = "localhost:8080";
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
//        checkChildren(result, file);
//        Long len = file.getContentLength();
//        assertEquals(len, new Long("DATA".getBytes().length));
//
//        String acceps = "text/html,text/*;q=0.9";
//        String res = file.getContentType(acceps);
//        String expResult = "text/*;q=0.9";
//        assertEquals(expResult, res);
//
//        Date date = file.getCreateDate();
//        assertNotNull(date);
//        date = file.getModifiedDate();
//        assertNotNull(date);
//
//
//        String name = file.getName();
//        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
//
//        result.delete();
//        result = (WebDataDirResource) (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//
//        instance = new WebDataResourceFactory();
//
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//        bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
//        checkChildren(result, file);
//        len = file.getContentLength();
//        assertEquals(len, new Long("DATA".getBytes().length));
//
//        acceps = "text/html,text/*;q=0.9";
//        res = file.getContentType(acceps);
//        expResult = "text/*;q=0.9";
//        assertEquals(expResult, res);
//
//        date = file.getCreateDate();
//        assertNotNull(date);
//        date = file.getModifiedDate();
//        assertNotNull(date);
//
//
//        name = file.getName();
//        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
//
//        result.delete();
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//
//    }
//
//    @Test
//    public void testCreateGetAndDeleteResource2() throws Exception {
//
//        System.out.println("getResource");
//        String host = "localhost:8080";
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
//        checkChildren(result, file);
//        Long len = file.getContentLength();
//        assertEquals(len, new Long("DATA".getBytes().length));
//
//        String acceps = "text/html,text/*;q=0.9";
//        String res = file.getContentType(acceps);
//        String expResult = "text/*;q=0.9";
//        assertEquals(expResult, res);
//
//        Date date = file.getCreateDate();
//        assertNotNull(date);
//        date = file.getModifiedDate();
//        assertNotNull(date);
//
//        String name = file.getName();
//        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
//        List<? extends Resource> children = result.getChildren();
//        assertFalse(children.isEmpty());
//        boolean foundIt = false;
//        for (Resource r : children) {
//            if (r.getUniqueId().equals(file.getUniqueId())) {
//                foundIt = true;
//            }
//        }
//        assertTrue(foundIt);
//
//        file.delete();
//        SimpleDLCatalogue cat = new SimpleDLCatalogue();
//        ILogicalData entry = cat.getResourceEntryByLDRI(file.getPath());
//        assertNull(entry);
//
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//        Collection<IStorageSite> sites = file.getStorageSites();
//        assertFalse(sites.isEmpty());
//
////        System.out.println(">>>>>>Sites: " + sites.size());
////        for (IStorageSite s : sites) {
////            System.out.println(">>>>>>Sites: " + s.getEndpoint() + " " + s.getUID());
////        }
//        
//        file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
//        checkChildren(result, file);
//        len = file.getContentLength();
//        assertEquals(len, new Long("DATA".getBytes().length));
//
//
//        acceps = "text/html,text/*;q=0.9";
//        res = file.getContentType(acceps);
//        expResult = "text/*;q=0.9";
//        assertEquals(expResult, res);
//
//        date = file.getCreateDate();
//        assertNotNull(date);
//        date = file.getModifiedDate();
//        assertNotNull(date);
//
//
//        name = file.getName();
//        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
//
//        result.delete();
////        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
////        assertNull(result);
//        cat = new SimpleDLCatalogue();
//        entry = cat.getResourceEntryByLDRI(result.getPath());
//        assertNull(entry);
//
//    }
//
//    @Test
//    public void testStorageSites() throws Exception {
//
//        System.out.println("testStorageSites");
//        String host = "localhost:8080";
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//        
//        instance = new WebDataResourceFactory();
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//        result.delete();
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//    }
//
//    @Test
//    public void testCreateAndGetResourceContent() throws Exception {
//        System.out.println("testCreateAndGetResourceContent");
//        String host = "localhost:8080";
//
//        //1st PUT
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        if (result == null) {
//            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            assertNotNull(root);
//            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        }
//        checkResource(result);
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(result, file);
//
//        Long len = file.getContentLength();
//        assertEquals(len, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length));
//
//        //1st GET
//        instance = new WebDataResourceFactory();
//        file = (WebDataFileResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1 + "/" + ConstantsAndSettings.TEST_FILE_NAME_1);
//        assertNotNull(file);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        Range range = null;
//        Map<String, String> params = null;
//        String contentType = "text/plain";
//        file.sendContent(out, range, params, contentType);
//        String content = new String(out.toByteArray());
//        assertEquals(ConstantsAndSettings.TEST_DATA, content);
//
//
//        result.delete();
//        result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//    }
    @Test
    public void testMultiThread() {
        try {
            System.out.println("testMultiThread");
            Thread userThread1 = new UserThread(1);
            userThread1.setName("T1");

//            Thread userThread2 = new UserThread(1);
//            userThread2.setName("T2");


            userThread1.start();
//            userThread2.start();

            userThread1.join();
//            userThread2.join();
        } catch (InterruptedException ex) {
            fail();
            Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void checkChildren(WebDataDirResource result, WebDataFileResource file) {
        List<? extends Resource> children = result.getChildren();
        assertFalse(children.isEmpty());
        boolean foundIt = false;
        for (Resource r : children) {
            if (r.getUniqueId().equals(file.getUniqueId())) {
                foundIt = true;
            }
        }
        assertTrue(foundIt);
    }

    private void checkResource(WebDataDirResource result) throws VlException {
        assertNotNull(result);
        Collection<IStorageSite> sites = result.getStorageSites();
        assertFalse(sites.isEmpty());
        for (IStorageSite s : sites) {
            VFSNode node = s.createVFSFile(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertNotNull(node);
            assertTrue(node.exists());
            node = s.getVNode(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertNotNull(node);
            assertTrue(node.exists());

            s.deleteVNode(ConstantsAndSettings.TEST_FILE_PATH_1);

            assertFalse(node.exists());
            boolean hasData = s.LDRIHasPhysicalData(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertFalse(hasData);
        }
    }
    
}
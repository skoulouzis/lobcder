/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VChecksum;
import nl.uva.vlet.vfs.VFSNode;
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
public class LogicalDataTest {

    private Path path;
    private Path child;
    private ArrayList<StorageSite> sites;
    private StorageSite site;

    public LogicalDataTest() {
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
            path = Path.path("testPath");
            child = Path.path("testChildPath");

            sites = new ArrayList<StorageSite>();
            site = new StorageSite("file:///" + System.getProperty("java.io.tmpdir"), new Credential("user1"));
            sites.add(site);
        } catch (Exception ex) {
            Logger.getLogger(LogicalDataTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getLDRI method, of class LogicalData.
     */
    @Test
    public void testGetLDRI() {
        System.out.println("getLDRI");
        LogicalData instance = new LogicalData(path);

        Path expResult = path;
        Path result = instance.getLDRI();
        assertEquals(expResult, result);
    }

    /**
     * Test of getChildren method, of class LogicalData.
     */
    @Test
    public void testGetChildren() {
        System.out.println("getChildren");
        LogicalData instance = new LogicalData(path);
        instance.addChild(child);

        ArrayList<Path> result = instance.getChildren();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(child));
    }

    /**
     * Test of getStorageSites method, of class LogicalData.
     */
    @Test
    public void testGetStorageSites() {
        System.out.println("getStorageSites");
        LogicalData instance = new LogicalData(path);
        instance.setStorageSites(sites);

        ArrayList result = instance.getStorageSites();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(site));
    }

    /**
     * Test of getMetadata method, of class LogicalData.
     */
    @Test
    public void testGetMetadata() {
        System.out.println("getMetadata");
        LogicalData instance = new LogicalData(path);
        Metadata result = instance.getMetadata();
        assertNotNull(result);
    }

    /**
     * Test of setMetadata method, of class LogicalData.
     */
    @Test
    public void testSetMetadata() {
        System.out.println("setMetadata");
        Metadata metadata = new Metadata();
        metadata.setLength(Long.MIN_VALUE);
        LogicalData instance = new LogicalData(path);
        instance.setMetadata(metadata);
        Metadata result = instance.getMetadata();

        assertNotNull(result);
        assertEquals(metadata.getLength(), result.getLength());

    }

    /**
     * Test of getUID method, of class LogicalData.
     */
    @Test
    public void testGetUID() {
        System.out.println("getUID");
        LogicalData instance = new LogicalData(path);
        String result = instance.getUID();
        assertNotNull(result);
    }

    /**
     * Test of addChildren method, of class LogicalData.
     */
    @Test
    public void testAddChildren() {
        System.out.println("addChildren");
        ArrayList<Path> children = new ArrayList<Path>();
        children.add(child);
        LogicalData instance = new LogicalData(path);
        instance.addChildren(children);
        assertTrue(instance.hasChildren());
    }

    /**
     * Test of setStorageSites method, of class LogicalData.
     */
    @Test
    public void testSetStorageSites() {
        try {
            System.out.println("setStorageSites");
            LogicalData instance = new LogicalData(path);
            instance.setStorageSites(sites);
            assertFalse(instance.hasPhysicalData());

            site.createVFSFile(path);
            instance.setStorageSites(sites);
            assertTrue(instance.hasPhysicalData());
        } catch (VlException ex) {
            Logger.getLogger(LogicalDataTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of addChild method, of class LogicalData.
     */
    @Test
    public void testAddChild() {
        System.out.println("addChild");
        LogicalData instance = new LogicalData(path);
        instance.addChild(child);
        assertTrue(instance.hasChildren());
    }

    /**
     * Test of hasChildren method, of class LogicalData.
     */
    @Test
    public void testHasChildren() {
        System.out.println("hasChildren");
        LogicalData instance = new LogicalData(path);
        boolean result = instance.hasChildren();
        assertFalse(result);

        instance.addChild(child);
        result = instance.hasChildren();
        assertTrue(result);

    }

    /**
     * Test of removeChild method, of class LogicalData.
     */
    @Test
    public void testRemoveChild() {
        System.out.println("removeChild");

        LogicalData instance = new LogicalData(path);
        instance.addChild(child);
        boolean result = instance.hasChildren();
        assertTrue(result);

        instance.removeChild(child);

        result = instance.hasChildren();
        assertFalse(result);
    }

    /**
     * Test of getChild method, of class LogicalData.
     */
    @Test
    public void testGetChild() {
        System.out.println("getChild");
        LogicalData instance = new LogicalData(path);
        instance.addChild(child);

        Path result = instance.getChild(child);
        assertEquals(child, result);
    }

    /**
     * Test of setLDRI method, of class LogicalData.
     */
    @Test
    public void testSetLDRI() {
        System.out.println("setLDRI");
        Path newPath = Path.path("newPath");
        LogicalData instance = new LogicalData(path);

        instance.setLDRI(newPath);

        assertEquals(newPath, instance.getLDRI());
    }

//    /**
//     * Test of isRedirectAllowed method, of class LogicalData.
//     */
//    @Test
//    public void testIsRedirectAllowed() {
//        System.out.println("isRedirectAllowed");
//        LogicalData instance = null;
//        boolean expResult = false;
//        boolean result = instance.isRedirectAllowed();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of getVNode method, of class LogicalData.
     */
    @Test
    public void testGetVNode() throws Exception {
        System.out.println("getVNode");
        LogicalData instance = new LogicalData(path);

        VFSNode expResult = site.createVFSFile(path);
        instance.setStorageSites(sites);

//        System.err.println(">>>>>. NodeExp: " + expResult.getVRL());

        VFSNode result = instance.getVNode();

//        System.err.println(">>>>>. NodeResult: " + result.getVRL());

        assertEquals(expResult.getVRL(), result.getVRL());

        if (result instanceof VChecksum && expResult instanceof VChecksum) {
            String type = ((VChecksum) result).getChecksumTypes()[0];
            String checksum1 = ((VChecksum) result).getChecksum(type);

            String checksum2 = ((VChecksum) expResult).getChecksum(type);
            assertEquals(checksum1, checksum2);
        }

    }

    /**
     * Test of hasPhysicalData method, of class LogicalData.
     */
    @Test
    public void testHasPhysicalData() {
        try {
            System.out.println("hasPhysicalData");
            LogicalData instance = new LogicalData(path);
            boolean result = instance.hasPhysicalData();
            assertFalse(result);

            instance.setStorageSites(sites);
            result = instance.hasPhysicalData();
            assertFalse(result);


            VFSNode node = site.createVFSFile(path);
            assertNotNull(node);
            instance.setStorageSites(sites);
            result = instance.hasPhysicalData();
            assertTrue(result);


        } catch (VlException ex) {
            Logger.getLogger(LogicalDataTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of createPhysicalData method, of class LogicalData.
     */
    @Test
    public void testCreatePhysicalData() throws Exception {
        System.out.println("createPhysicalData");
        LogicalData instance = new LogicalData(path);
        VFSNode result = instance.createPhysicalData();
        assertNull(result);

        instance.setStorageSites(sites);
        result = instance.createPhysicalData();
        assertNotNull(result);
        
        assertTrue(result.exists());
    }
}
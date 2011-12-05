/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
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
public class StorageSiteTest {
    
    public StorageSiteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getVNode method, of class StorageSite.
     */
    @Test
    public void testGetVNode() throws Exception {
        System.out.println("getVNode");
        Path path = null;
        StorageSite instance = null;
        VFSNode expResult = null;
        VFSNode result = instance.getVNode(path);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of createVFSNode method, of class StorageSite.
     */
    @Test
    public void testCreateVFSNode() throws Exception {
        System.out.println("createVFSNode");
        Path path = null;
        StorageSite instance = null;
        VFSNode expResult = null;
        VFSNode result = instance.createVFSNode(path);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getEndpoint method, of class StorageSite.
     */
    @Test
    public void testGetEndpoint() {
        System.out.println("getEndpoint");
        StorageSite instance = null;
        String expResult = "";
        String result = instance.getEndpoint();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getVPHUsername method, of class StorageSite.
     */
    @Test
    public void testGetVPHUsername() {
        System.out.println("getVPHUsername");
        StorageSite instance = null;
        String expResult = "";
        String result = instance.getVPHUsername();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}

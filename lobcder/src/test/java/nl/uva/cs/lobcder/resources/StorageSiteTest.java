/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.io.File;
import com.bradmcevoy.common.Path;
import java.util.Collection;
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

    private static String name = "storage3.prop";
    private final String testEndpoint;
    private final Credential testCred;

    public StorageSiteTest() throws FileNotFoundException, IOException {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;


        Properties prop = getCloudProperties(propBasePath + name);
        testEndpoint = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.STORAGE_SITE_ENDPOINT);
        
        testCred = new Credential();
        String siteUname = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.STORAGE_SITE_USERNAME);
        testCred.setStorageSiteUsername(siteUname);
        String passwd = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.STORAGE_SITE_PASSWORD);
        testCred.setStorageSitePassword(passwd);
    }

    private static Properties getCloudProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
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
     * Test of createVFSNode method, of class StorageSite.
     */
    @Test
    public void testCreateAndGetVFSNode() throws Exception {
        System.out.println("createVFSNode");
        Path path = Path.path("file1]");
        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        VFSNode expResult = null;
        VFSNode result = instance.createVFSNode(path);
//        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

//    /**
//     * Test of getEndpoint method, of class StorageSite.
//     */
//    @Test
//    public void testGetEndpoint() {
//        System.out.println("getEndpoint");
//        StorageSite instance = null;
//        String expResult = "";
//        String result = instance.getEndpoint();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getVPHUsername method, of class StorageSite.
//     */
//    @Test
//    public void testGetVPHUsername() {
//        System.out.println("getVPHUsername");
//        StorageSite instance = null;
//        String expResult = "";
//        String result = instance.getVPHUsername();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getLogicalPaths method, of class StorageSite.
//     */
//    @Test
//    public void testGetLogicalPaths() {
//        System.out.println("getLogicalPaths");
//        StorageSite instance = null;
//        Collection expResult = null;
//        Collection result = instance.getLogicalPaths();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}

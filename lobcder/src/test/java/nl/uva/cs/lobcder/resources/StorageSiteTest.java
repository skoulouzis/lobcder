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
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesLoader;
import nl.uva.vlet.vfs.VFSNode;
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
public class StorageSiteTest {

//    private static String name = "storage1.prop";
    private final String testEndpoint;
    private final Credential testCred;
    private final String vphUser;

    public StorageSiteTest() throws FileNotFoundException, IOException {
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder"
//                + File.separator + "etc" + File.separator;


//        Properties prop = getCloudProperties(propBasePath + name);
        Properties prop = PropertiesLoader.getStorageSitesProps()[0];
        testEndpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
        vphUser = prop.getProperty(Constants.VPH_USERNAME);

        testCred = new Credential(vphUser);
        String siteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
        testCred.setStorageSiteUsername(siteUname);
        String passwd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
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
        System.out.println("testCreateAndGetVFSNode");
        String strPath = "file1";
        Path path = Path.path(strPath);
        StorageSite instance = new StorageSite(testEndpoint, testCred);
        VFSNode result = instance.createVFSFile(path);
        assertNotNull(result);
        VFSNode node = instance.getVNode(Path.path(strPath));
        assertNotNull(node);

        assertEquals(node.getVRL(), result.getVRL());
    }

    /**
     * Test of getEndpoint method, of class StorageSite.
     */
    @Test
    public void testGetEndpoint() throws Exception {
        System.out.println("getEndpoint");
        StorageSite instance = new StorageSite(testEndpoint, testCred);
        String result = instance.getEndpoint();
        System.out.println("endpoint: " + result);
        assertEquals(testEndpoint, result);
    }

    /**
     * Test of getVPHUsername method, of class StorageSite.
     */
    @Test
    public void testGetVPHUsername() throws Exception {
        System.out.println("getVPHUsername");
        StorageSite instance = new StorageSite(testEndpoint, testCred);
        String result = instance.getVPHUsername();
        assertEquals(vphUser, result);
    }

    /**
     * Test of getLogicalPaths method, of class StorageSite.
     */
    @Test
    public void testGetLogicalPaths() throws Exception {
        System.out.println("getLogicalPaths");
        StorageSite instance = new StorageSite(testEndpoint, testCred);
        String strPath1 = "file1";
        Path path1 = Path.path(strPath1);
        String strPath2 = "file2";
        Path path2 = Path.path(strPath2);

        instance.createVFSFile(path1);
        instance.createVFSFile(path2);

        Collection<String> result = instance.getLogicalPaths();
        assertNotNull(result);

        assertTrue(result.contains(strPath1));
        assertTrue(result.contains(strPath2));

    }

    /**
     * Test of createVFSNode method, of class StorageSite.
     */
    @Test
    public void testCreateAndGetVFSFolder() throws Exception {
        System.out.println("testCreateAndGetVFSFolder");
        String strPath = "file1";
        Path path = Path.path(strPath);
        StorageSite instance = new StorageSite(testEndpoint, testCred);
        VFSNode result = instance.createVFSFile(path);
        assertNotNull(result);
        VFSNode node = instance.getVNode(Path.path(strPath));
        assertNotNull(node);
        assertEquals(node.getVRL(), result.getVRL());


        strPath = "file3/file4";
        path = Path.path(strPath);

        result = instance.createVFSFile(path);
        assertNotNull(result);
        node = instance.getVNode(Path.path(strPath));
        assertNotNull(node);
    }
}

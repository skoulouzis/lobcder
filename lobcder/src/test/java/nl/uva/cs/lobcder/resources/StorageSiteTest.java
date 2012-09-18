///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package nl.uva.cs.lobcder.resources;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.FileNotFoundException;
//import java.util.Properties;
//import java.io.File;
//import com.bradmcevoy.common.Path;
//import java.util.Collection;
//import nl.uva.cs.lobcder.util.Constants;
//import nl.uva.cs.lobcder.util.PropertiesLoader;
//import nl.uva.vlet.exception.VlException;
//import nl.uva.vlet.vfs.VFSNode;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
///**
// *
// * @author S. koulouzis
// */
//public class StorageSiteTest {
//
////    private static String name = "storage1.prop";
//    private final String testEndpoint;
//    private final Credential testCred;
//    private final String[] vphUsers;
//
//    public StorageSiteTest() throws FileNotFoundException, IOException {
////        String propBasePath = System.getProperty("user.home") + File.separator
////                + "workspace" + File.separator + "lobcder"
////                + File.separator + "etc" + File.separator;
//
//
////        Properties prop = getCloudProperties(propBasePath + name);
//        Properties prop = PropertiesLoader.getStorageSitesProps()[0];
//        testEndpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
//        vphUsers = prop.getProperty(Constants.VPH_USERNAMES).split(",");
//
//        testCred = new Credential(vphUsers);
//        String siteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
//        testCred.setStorageSiteUsername(siteUname);
//        String passwd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
//        testCred.setStorageSitePassword(passwd);
//    }
//
//    private static Properties getCloudProperties(String propPath)
//            throws FileNotFoundException, IOException {
//        Properties properties = new Properties();
//
//        File f = new File(propPath);
//        properties.load(new FileInputStream(f));
//
//        return properties;
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
//    }
//
//    @After
//    public void tearDown() {
//    }
//
//    /**
//     * Test of createVFSNode method, of class StorageSite.
//     */
//    @Test
//    public void testCreateAndGetVFSNode() throws Exception {
//        System.out.println("testCreateAndGetVFSNode");
//        String strPath = "file1";
//        Path path = Path.path(strPath);
//        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        VFSNode result = instance.createVFSFile(path);
//        assertNotNull(result);
//        VFSNode node = instance.getVNode(Path.path(strPath));
//        assertNotNull(node);
//
//        assertEquals(node.getVRL(), result.getVRL());
//    }
//
//    /**
//     * Test of getEndpoint method, of class StorageSite.
//     */
//    @Test
//    public void testGetEndpoint() throws Exception {
//        System.out.println("getEndpoint");
//        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        String result = instance.getEndpoint();
//        System.out.println("endpoint: " + result);
//        assertEquals(testEndpoint, result);
//    }
//
//    /**
//     * Test of getVPHUsername method, of class StorageSite.
//     */
//    @Test
//    public void testGetVPHUsername() throws Exception {
//        System.out.println("getVPHUsername");
//        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        Collection<String> result = instance.getVPHUsernames();
//        for (String s : vphUsers) {
//            assertTrue(result.contains(s));
//        }
//    }
//
//    /**
//     * Test of getLogicalPaths method, of class StorageSite.
//     */
//    @Test
//    public void testGetLogicalPaths() throws Exception {
//        System.out.println("getLogicalPaths");
//        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        String strPath1 = "file1";
//        Path path1 = Path.path(strPath1);
//        String strPath2 = "file2";
//        Path path2 = Path.path(strPath2);
//
//        instance.createVFSFile(path1);
//        instance.createVFSFile(path2);
//
//        Collection<String> result = instance.getLogicalPaths();
//        assertNotNull(result);
//
//        assertTrue(result.contains(strPath1));
//        assertTrue(result.contains(strPath2));
//
//    }
//
//    /**
//     * Test of createVFSNode method, of class StorageSite.
//     */
//    @Test
//    public void testCreateAndGetVFSFolder() throws Exception {
//        System.out.println("testCreateAndGetVFSFolder");
//        String strPath = "file1";
//        Path path = Path.path(strPath);
//        StorageSite instance = new StorageSite(testEndpoint, testCred);
//        VFSNode result = instance.createVFSFile(path);
//        assertNotNull(result);
//        VFSNode node = instance.getVNode(Path.path(strPath));
//        assertNotNull(node);
//        assertEquals(node.getVRL(), result.getVRL());
//
//
//        strPath = "file3/file4";
//        path = Path.path(strPath);
//
//        result = instance.createVFSFile(path);
//        assertNotNull(result);
//        node = instance.getVNode(Path.path(strPath));
//        assertNotNull(node);
//    }
//
//    @Test
//    public void testLocalStorageSite() throws Exception {
//        String localEndpoint = "file:///tmp/";
//        Credential cred = new Credential(new String[]{"names"});
//        StorageSite localSS = new StorageSite(localEndpoint, cred);
//        testStorageSite(localSS);
//    }
//
//    @Test
//    /**
//     * To run this test you mast create in the
//     * <code>Constants.LOBCDER_CONF_DIR</code> the files sftp_local.prop,
//     * sftp_cyfornet.prop and sftp_ui.sara.prop for ssh see
//     * Doc/Config_Instructions on how to create these files
//     */
//    public void testSFTPStorageSite() throws Exception {
//
//        Properties prop = new Properties();
//
//        File sftpLocal = new File(Constants.LOBCDER_CONF_DIR + "sftp_local.prop");
//        prop.load(new FileInputStream(sftpLocal));
//        String endpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
//        String[] users = prop.getProperty(Constants.VPH_USERNAMES).split(",");
//        Credential cred = new Credential(users);
//        String siteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
//        cred.setStorageSiteUsername(siteUname);
//        String passwd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
//        cred.setStorageSitePassword(passwd);
//        StorageSite sftpSS = new StorageSite(endpoint, cred);
//        testStorageSite(sftpSS);
//
//
//        File sftpCyfornet = new File(Constants.LOBCDER_CONF_DIR + "sftp_cyfornet.prop");
//        prop.load(new FileInputStream(sftpCyfornet));
//        String cyfornetEndpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
//        String[] cyfornetUsers = prop.getProperty(Constants.VPH_USERNAMES).split(",");
//        Credential cyfornetCred = new Credential(cyfornetUsers);
//        String cyfornetSiteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
//        cyfornetCred.setStorageSiteUsername(cyfornetSiteUname);
//        String cyfornetPasswd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
//        cyfornetCred.setStorageSitePassword(cyfornetPasswd);
//        StorageSite cyfornetSftpSS = new StorageSite(cyfornetEndpoint, cyfornetCred);
//        testStorageSite(cyfornetSftpSS);
//
//        File sftpSara = new File(Constants.LOBCDER_CONF_DIR + "sftp_ui.sara.prop");
//        prop.load(new FileInputStream(sftpSara));
//        String saraEndpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
//        String[] saraUsers = prop.getProperty(Constants.VPH_USERNAMES).split(",");
//        Credential saraCred = new Credential(saraUsers);
//        String saraSiteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
//        saraCred.setStorageSiteUsername(saraSiteUname);
//        String saraPasswd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
//        saraCred.setStorageSitePassword(saraPasswd);
//        StorageSite saraSftpSS = new StorageSite(saraEndpoint, saraCred);
//        testStorageSite(saraSftpSS);
//    }
//    
//    
//    
//      @Test
//    /**
//     * To run this test you mast create in the
//     * <code>Constants.LOBCDER_CONF_DIR</code> the file swift.prop.prop,
//     * for swift see Doc/Config_Instructions on how to create these files
//     */
//    public void testSWIFTStorageSite() throws Exception {
//
//        Properties prop = new Properties();
//
//        File swiftCyfornet = new File(Constants.LOBCDER_CONF_DIR + "swift.prop");
//        prop.load(new FileInputStream(swiftCyfornet));
//        String cyfornetEndpoint = prop.getProperty(Constants.STORAGE_SITE_ENDPOINT);
//        String[] cyfornetUsers = prop.getProperty(Constants.VPH_USERNAMES).split(",");
//        Credential cyfornetCred = new Credential(cyfornetUsers);
//        String cyfornetSiteUname = prop.getProperty(Constants.STORAGE_SITE_USERNAME);
//        cyfornetCred.setStorageSiteUsername(cyfornetSiteUname);
//        String cyfornetPasswd = prop.getProperty(Constants.STORAGE_SITE_PASSWORD);
//        cyfornetCred.setStorageSitePassword(cyfornetPasswd);
//        StorageSite cyfornetSwift = new StorageSite(cyfornetEndpoint, cyfornetCred);
//        testStorageSite(cyfornetSwift);
//    }
//
//    private void testStorageSite(StorageSite site) throws VlException {
//        String strPath = "file1";
//        Path path = Path.path(strPath);
//        VFSNode result = site.createVFSFile(path);
//        assertNotNull(result);
//        assertTrue(site.LDRIHasPhysicalData(path));
//        VFSNode node = site.getVNode(Path.path(strPath));
//        assertNotNull(node);
//        assertEquals(node.getVRL(), result.getVRL());
//
//
//        site.deleteVNode(path);
//        assertFalse(site.LDRIHasPhysicalData(path));
//        node = site.getVNode(Path.path(strPath));
//        assertNull(node);
//    }
//}

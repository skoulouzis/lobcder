/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Collection;
import java.util.Properties;
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
public class StorageSiteManagerTest {

    private static String[] names = new String[]{"storage2.prop", "storage4.prop"};
    private String vphUname;

    public StorageSiteManagerTest() {
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
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetAllSites() throws Exception {
        System.out.println("testGetAllSites");
        populateStorageSites();
        
        StorageSiteManager instance = new StorageSiteManager();
        try {
            Collection<StorageSite> result = instance.getAllSites();
            assertNotNull(result);

            assertEquals(names.length, result.size());
            for (StorageSite s : result) {
                System.out.println("Site: " + s.getEndpoint());
            }

        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetSitesByUnames() throws Exception {
        System.out.println("testGetSitesByUnames");
        populateStorageSites();

        StorageSiteManager instance = new StorageSiteManager();
        try {
            Collection<StorageSite> result = instance.getSitesByUname(vphUname);
            assertNotNull(result);
        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetSitesByLPath() throws Exception {
        System.out.println("testGetSitesByLPath");
        populateStorageSites();

        StorageSiteManager instance = new StorageSiteManager();
        Collection<String> paths;
        try {
            Collection<StorageSite> allSites = instance.getAllSites();
            Path p1 = Path.path("path1");
            Path p2 = Path.path("path2");
            Path p3 = Path.path("path2/path3");
            
            for (StorageSite s : allSites) {
                System.out.println("Site: " + s.getEndpoint());
                s.createVFSNode(p1);
                s.createVFSNode(p2);
                s.createVFSNode(p3);
                paths = s.getLogicalPaths();
                for (String path : paths) {
                    assertNotNull(path);
//                    System.out.println("\t path: " + path);
                }
            }
                        
            Collection<StorageSite> res = instance.getSitesByUname(vphUname);
            
        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    private void populateStorageSites() throws FileNotFoundException, IOException, Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;

        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            vphUname = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.VPH_USERNAME);
            StorageSiteManager instance = new StorageSiteManager();
            instance.registerStorageSite(prop);
        }
    }

    private static Properties getCloudProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
    }
}

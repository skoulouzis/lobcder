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
import java.util.ArrayList;
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
 * @author skoulouz
 */
public class StorageSiteManagerTest {

    private static String[] names = new String[]{"storage2.prop", "storage3.prop"};
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
//            for (StorageSite s : result) {
//                System.out.println("Site: " + s.getEndpoint());
//            }

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
            
            
            //            Collection<StorageSite> result = instance.getSitesByLPath(Path.root);
            //            assertNotNull(result);
            Collection<StorageSite> allSites = instance.getAllSites();
            for (StorageSite s : allSites) {
                System.out.println("Site: " + s.getEndpoint());
                s.createVFSNode(Path.path("path1"));
                s.createVFSNode(Path.path("path2"));
                s.createVFSNode(Path.path("path2/path3"));
                paths = s.getLogicalPaths();
                for (String path : paths) {
                    System.out.println("\t path: " + path);
                }
            }
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

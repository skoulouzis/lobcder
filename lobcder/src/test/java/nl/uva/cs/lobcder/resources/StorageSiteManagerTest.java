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
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.webdav.Constants.Constants;
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
    private ArrayList<String> endpoints;
    private Collection<Properties> props = new ArrayList<Properties>();

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
        try {
            populateStorageSites();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StorageSiteManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(StorageSiteManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(StorageSiteManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
    }

    @After
    public void tearDown() {
        new StorageSiteManager().clearAllSites();
    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetAllSites() throws Exception {
        System.out.println("testGetAllSites");

        StorageSiteManager instance = new StorageSiteManager();
        try {
            Collection<StorageSite> result = instance.getAllSites();
            assertNotNull(result);

            for (StorageSite s : result) {
                System.out.println("StorageSite: " + s.getEndpoint() + " endpoint: " + endpoints);
                assertTrue(endpoints.contains(s.getEndpoint()));
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
    public void testDeleteStorgaeSites() throws Exception {
        System.out.println("testGetSitesByUnames");

        StorageSiteManager instance = new StorageSiteManager();
        try {
            Collection<StorageSite> result = instance.getSitesByUname(vphUname);
            assertNotNull(result);

            instance.deleteStorgaeSites(result);

            result = instance.getSitesByUname(vphUname);
            assertTrue(result.isEmpty());

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
    public void testStorageSiteExists() throws Exception {
        System.out.println("testStorageSiteExists");
        StorageSiteManager instance = new StorageSiteManager();
        
        Properties p = props.iterator().next();
        
        
        boolean exists = instance.storageSiteExists(p);
        
        assertTrue(exists);
        
        instance.deleteStorgaeSite(p);
        
        exists = instance.storageSiteExists(p);
        
        assertFalse(exists);

    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetSitesByLPath() throws Exception {
        System.out.println("testGetSitesByLPath");
        Path p1, p2, p3;

        StorageSiteManager instance = new StorageSiteManager();
        Collection<String> paths;
        boolean foundIt = false;
        try {
            Collection<StorageSite> allSites = instance.getAllSites();
            StorageSite[] sitesArray = new StorageSite[allSites.size()];
            sitesArray = allSites.toArray(sitesArray);
            p1 = Path.path("path1");
            p2 = Path.path("path2");
            p3 = Path.path("path3/path4/path5");

            sitesArray[0].createVFSFile(p1);
            paths = sitesArray[0].getLogicalPaths();
            for (String path : paths) {
                assertNotNull(path);
                if (path.equals(p1.toString())) {
                    foundIt = true;
                }
            }
            assertTrue(foundIt);

            sitesArray[1].createVFSFile(p2);
            paths = sitesArray[1].getLogicalPaths();
            for (String path : paths) {
                assertNotNull(path);
                if (path.equals(p2.toString())) {
                    foundIt = true;
                }
            }
            assertTrue(foundIt);

            sitesArray[0].createVFSFile(p3);
            paths = sitesArray[0].getLogicalPaths();
            for (String path : paths) {
                assertNotNull(path);
                if (path.equals(p3.toString())) {
                    foundIt = true;
                }
            }
            assertTrue(foundIt);

            Collection<StorageSite> res = instance.getSitesByLPath(p1);
            for (StorageSite s : res) {
//                System.out.println("endpoints: " + s.getEndpoint());
                assertNotNull(s);
                assertTrue(s.getLogicalPaths().contains(p1.toString()));
            }

            res = instance.getSitesByLPath(p2);

            for (StorageSite s : res) {
//                System.out.println("endpoints: " + s.getEndpoint());
                assertNotNull(s);
                assertTrue(s.getLogicalPaths().contains(p2.toString()));
            }

            res = instance.getSitesByLPath(p3);

            for (StorageSite s : res) {
//                System.out.println("endpoints: " + s.getEndpoint());
                assertNotNull(s);
                assertTrue(s.getLogicalPaths().contains(p3.toString()));
            }



        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    private void populateStorageSites() throws FileNotFoundException, IOException, Exception {
        new StorageSiteManager().clearAllSites();
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;

        endpoints = new ArrayList<String>();
        StorageSiteManager instance = new StorageSiteManager();

        props.clear();
        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            vphUname = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.VPH_USERNAME);
            this.props.add(prop);
            endpoints.add(prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.STORAGE_SITE_ENDPOINT));
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

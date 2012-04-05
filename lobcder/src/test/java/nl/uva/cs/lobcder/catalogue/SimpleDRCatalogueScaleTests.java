    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.webDav.resources.Constants;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author S. Koulouzis
 */
public class SimpleDRCatalogueScaleTests {

//    private static String[] names = new String[]{"storage1.prop", "storage2.prop", "storage3.prop"};
    private static String[] names = new String[]{"storage1.prop"};

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

    @Test
    public void testGetSitesByUnames() throws Exception {
        System.out.println("testGetSitesByUnames");
        populateStorageSites();
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        try {
            String uname = "uname2";
            Properties prop = new Properties();
            prop.setProperty(Constants.VPH_USERNAME, uname);
            prop.setProperty(Constants.STORAGE_SITE_USERNAME, "vph_dev:user");
            prop.setProperty(Constants.STORAGE_SITE_PASSWORD, "non");
            prop.setProperty(Constants.STORAGE_SITE_ENDPOINT, "file:///" + System.getProperty("user.home") + "/deleteMe/");

            instance.registerStorageSite(prop);
            boolean exists = instance.storageSiteExists(prop);
            assertTrue(exists);

            Collection<IStorageSite> result = instance.getSitesByUname(uname);
            assertNotNull(result);
            assertFalse(result.isEmpty());

            for (IStorageSite s : result) {
                assertEquals( uname,s.getVPHUsername());
            }

        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    private void populateStorageSites() throws FileNotFoundException, IOException, Exception {
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        instance.clearAllSites();
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;
        ArrayList<String> endpoints = new ArrayList<String>();


        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            endpoints.add(prop.getProperty(Constants.STORAGE_SITE_ENDPOINT));
            instance.registerStorageSite(prop);
            boolean exists = instance.storageSiteExists(prop);
            assertTrue(exists);
        }
    }

    private static Properties getCloudProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));
        return properties;
    }

    private boolean compareEntries(ILogicalData entry, ILogicalData loadedEntry) {
        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
//            if (entry.getUID().equals(loadedEntry.getUID())) {
            return true;
//            }
        }
        return false;
    }
}

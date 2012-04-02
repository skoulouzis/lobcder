    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import java.io.FileInputStream;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.Metadata;
import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import nl.uva.cs.lobcder.webDav.resources.Constants;
import nl.uva.cs.lobcder.webDav.resources.UserThread;
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

    /**
     * Test of registerResourceEntry method, of class SimpleDRCatalogue.
     */
    @Test
    public void testRegisterResourceEntry() throws Exception {
        System.out.println("registerResourceEntry");
        //Register one resource
        String ldri = "resource1";
        Path path = Path.path(ldri);
        ILogicalData entry = new LogicalData(path);

        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        try {
            instance.registerResourceEntry(entry);
            System.out.println("entry:          " + entry.getLDRI());


            ILogicalData loadedEntry = instance.getResourceEntryByLDRI(path);
            assertNotNull(loadedEntry);
            
            System.out.println("LDRI: "+loadedEntry.getLDRI());

            boolean theSame = compareEntries(entry, loadedEntry);

            assertTrue(theSame);
            
        } catch (Exception ex) {
            if (!ex.getMessage().equals("registerResourceEntry: cannot register resource " + ldri + " resource exists")) {
                fail(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        } finally {
            instance.unregisterResourceEntry(entry);
            ILogicalData result = instance.getResourceEntryByLDRI(path);
            assertNull(result);
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

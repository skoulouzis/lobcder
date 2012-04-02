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
import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.webDav.resources.Constants;
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

//    @Test
//    public void testRegisterResourceEntry() throws Exception {
//        System.out.println("registerResourceEntry");
//        //Register one resource
//        String ldri = "resource1";
//        Path path = Path.path(ldri);
//        ILogicalData entry = new LogicalData(path);
//
//        SimpleDLCatalogue instance = new SimpleDLCatalogue();
//        try {
//            instance.registerResourceEntry(entry);
//            System.out.println("entry:          " + entry.getLDRI());
//            
//            ILogicalData loadedEntry = instance.getResourceEntryByLDRI(path);
//            assertNotNull(loadedEntry);
//            
//            System.out.println("LDRI: "+loadedEntry.getLDRI());
//
//            boolean theSame = compareEntries(entry, loadedEntry);
//
//            assertTrue(theSame);
//            
//        } catch (Exception ex) {
//            if (!ex.getMessage().equals("registerResourceEntry: cannot register resource " + ldri + " resource exists")) {
//                fail(ex.getMessage());
//            } else {
//                ex.printStackTrace();
//            }
//        } finally {
//            instance.unregisterResourceEntry(entry);
//            ILogicalData result = instance.getResourceEntryByLDRI(path);
//            assertNull(result);
//        }
//    }
    @Test
    public void testGetTopLevelResourceEntries() {
        System.out.println("getTopLevelResourceEntries");
        Collection<ILogicalData> topEntries = new ArrayList<ILogicalData>();
        LogicalData topEntry1 = new LogicalData(Path.path("/r1"));
        topEntries.add(topEntry1);
        LogicalData topEntry2 = new LogicalData(Path.path("/r2"));
        topEntries.add(topEntry2);
        LogicalData topEntry3 = new LogicalData(Path.path("/r3"));
        topEntries.add(topEntry3);

        LogicalData entry11 = new LogicalData(Path.path("/r1/r11"));
        LogicalData entry21 = new LogicalData(Path.path("/r2/r21"));
        SimpleDLCatalogue instance = null;
        Collection<ILogicalData> result = null;
        ILogicalData loaded;
        try {

            instance = new SimpleDLCatalogue();
            instance.registerResourceEntry(topEntry1);
            instance.registerResourceEntry(topEntry2);
            instance.registerResourceEntry(topEntry3);
            instance.registerResourceEntry(entry11);
            instance.registerResourceEntry(entry21);

            result = instance.getTopLevelResourceEntries();


            for (ILogicalData d : result) {
                System.out.println("TOP:        " + d.getLDRI() + "     " + d.getUID());

                if (!compareEntries(d, topEntry3)) {
                    if (!compareEntries(d, topEntry2)) {
                        if (!compareEntries(d, topEntry1)) {
                            fail("Resource " + topEntry1.getLDRI() + " is not returned by query!");
                        }
                    }
                }
            }

            for (ILogicalData e : result) {
                if (e.getLDRI().getLength() > 1) {
                    fail("Resource " + e.getLDRI() + " is not a top level");
                }
            }

        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                instance.unregisterResourceEntry(entry21);
                instance.unregisterResourceEntry(entry11);
                instance.unregisterResourceEntry(topEntry3);
                instance.unregisterResourceEntry(topEntry2);
                instance.unregisterResourceEntry(topEntry1);


                result = instance.getTopLevelResourceEntries();


                for (ILogicalData d : result) {
                    if (compareEntries(d, topEntry3) || compareEntries(d, topEntry2) || compareEntries(d, topEntry1)) {
                        fail("entry: " + d.getLDRI() + " should not be registered in the catalogue!");
                    }
                }


                loaded = instance.getResourceEntryByLDRI(entry21.getLDRI());
                assertNull(loaded);

                loaded = instance.getResourceEntryByLDRI(entry11.getLDRI());
                assertNull(loaded);

                loaded = instance.getResourceEntryByLDRI(topEntry3.getLDRI());
                assertNull(loaded);


                loaded = instance.getResourceEntryByLDRI(topEntry2.getLDRI());
                assertNull(loaded);

                loaded = instance.getResourceEntryByLDRI(topEntry1.getLDRI());
                assertNull(loaded);



            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
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

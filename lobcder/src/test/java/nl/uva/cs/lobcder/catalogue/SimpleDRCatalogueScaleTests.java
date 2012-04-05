    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import nl.uva.cs.lobcder.resources.*;
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
    public void testRegisterToExistiongParent() {
        System.out.println("testRegisterToExistiongParent");
        SimpleDLCatalogue instance = null;
        LogicalData lParent = null;
        try {

            instance = new SimpleDLCatalogue();
            Path parentPath = Path.path("parent");

            lParent = new LogicalFolder(parentPath);

            instance.registerResourceEntry(lParent);

        } catch (Exception ex) {
            fail("Exception: " + ex.getMessage());
        } 
        finally {
            try {
                if (lParent != null) {
                    new SimpleDLCatalogue().unregisterResourceEntry(lParent);
                }
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
        }
    }

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
//            System.out.println("entry:          " + entry.getLDRI());


            ILogicalData loadedEntry = instance.getResourceEntryByLDRI(path);
            assertNotNull(loadedEntry);

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

    private boolean compareEntries(ILogicalData entry, ILogicalData loadedEntry) {
//        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
//            if (entry.getUID().equals(loadedEntry.getUID())) {
            return true;
//            }
        }
        return false;
    }
}

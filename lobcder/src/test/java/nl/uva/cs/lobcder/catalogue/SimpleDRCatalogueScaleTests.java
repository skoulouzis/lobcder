    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.io.IOException;
import java.util.Collection;
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
        LogicalData lChild = null;
        LogicalData lParent = null;
        try {

            instance = new SimpleDLCatalogue();
            Path parentPath = Path.path("parent");
            Path childPath = Path.path("parent/child");

            lParent = new LogicalFolder(parentPath);
            lChild = new LogicalFile(childPath);

            instance.registerResourceEntry(lParent);

            instance.registerResourceEntry(lChild);
            ILogicalData res = instance.getResourceEntryByLDRI(childPath);
            boolean theSame = compareEntries(lChild, res);
            assertTrue(theSame);

        } catch (Exception ex) {
            fail("Exception: " + ex.getMessage());
        } finally {
            try {
                if (lChild != null) {
                    new SimpleDLCatalogue().unregisterResourceEntry(lChild);
                }
                if (lParent != null) {
                    new SimpleDLCatalogue().unregisterResourceEntry(lParent);
                }
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
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

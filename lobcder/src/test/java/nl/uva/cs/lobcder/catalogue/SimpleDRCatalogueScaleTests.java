    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
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
    public void testRenameWithChildren() {
        System.out.println("testRenameWithChildren");
        SimpleDLCatalogue instance = null;
        ILogicalData loaded = null;
        int foundIt = 0;
        String childName1 = "Child1";
        String childName2 = "Child2";

        LogicalData childEntry1 = null;
        LogicalData childEntry2 = null;
        ILogicalData childLoaded;
        ILogicalData parentLoaded;
        Path newPath = null;
        try {

            instance = new SimpleDLCatalogue();
            Path originalPath = Path.path("/oldResourceName/");
            LogicalData e = new LogicalData(originalPath);
            instance.registerResourceEntry(e);

            Path originalChildPath1 = Path.path("/oldResourceName/" + childName1);
            childEntry1 = new LogicalData(originalChildPath1);
            instance.registerResourceEntry(childEntry1);

            Path originalChildPath2 = Path.path("/oldResourceName/" + childName2);
            childEntry2 = new LogicalData(originalChildPath2);
            instance.registerResourceEntry(childEntry2);

            newPath = Path.path("/newResourceName");
            instance.renameEntry(originalPath, newPath);
            
            
            loaded = instance.getResourceEntryByLDRI(newPath);

            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());
            
            Collection<Path> children = loaded.getChildren();
            assertNotNull(children);
            assertFalse(children.isEmpty());

            for (Path p : children) {
                if (p.equals(Path.path(newPath, childName1)) || p.equals(Path.path(newPath, childName2))) {
                    foundIt++;
                }
            }
            assertEquals(foundIt, 2);

        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {

                childLoaded = instance.getResourceEntryByLDRI(Path.path(newPath, childName1));
                assertNotNull(childLoaded);
                instance.unregisterResourceEntry(childLoaded);
                childLoaded = instance.getResourceEntryByLDRI(Path.path(newPath, childName1));
                assertNull(childLoaded);
                

                childLoaded = instance.getResourceEntryByLDRI(Path.path(newPath, childName2));
                assertNotNull(childLoaded);
                instance.unregisterResourceEntry(childLoaded);
                childLoaded = instance.getResourceEntryByLDRI(Path.path(newPath, childName2));
                assertNull(childLoaded);


                instance.unregisterResourceEntry(loaded);
                parentLoaded = instance.getResourceEntryByLDRI(loaded.getLDRI());
                assertNull(parentLoaded);
            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
    }
}

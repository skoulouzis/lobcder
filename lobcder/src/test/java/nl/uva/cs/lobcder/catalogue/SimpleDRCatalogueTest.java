/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
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
public class SimpleDRCatalogueTest {

    public SimpleDRCatalogueTest() {
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
     * Test of registerResourceEntry method, of class SimpleDRCatalogue.
     */
    @Test
    public void testRegisterResourceEntry() throws Exception {
        System.out.println("registerResourceEntry");
        //Register one resource
        String ldri = "/resource1";
        Path path = Path.path(ldri);
        ILogicalData entry = new LogicalData(path);

        SimpleDRCatalogue instance = new SimpleDRCatalogue();
        try {
            instance.registerResourceEntry(entry);
            System.out.println("entry:          " + entry.getLDRI());
        } catch (Exception ex) {
            if (!ex.getMessage().equals("registerResourceEntry: cannot register resource " + ldri + " resource exists")) {
                fail(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        }

        ILogicalData loadedEntry = instance.getResourceEntryByLDRI(path);
        assertNotNull(loadedEntry);

        boolean theSame = compareEntries(entry, loadedEntry);

        assertTrue(theSame);

        instance.unregisterResourceEntry(entry);
        ILogicalData result = instance.getResourceEntryByLDRI(path);
        
        assertNull(result);
    }

    @Test
    public void testRegisterMultipleResourceEntry() throws Exception {
        System.out.println("testRegisterMultipleResourceEntry");
        String ldri = "/resource2";
        Path parentPath = Path.path(ldri);
        ILogicalData parent = new LogicalData(parentPath);
        
        SimpleDRCatalogue instance = new SimpleDRCatalogue();
        try {
            instance.registerResourceEntry(parent);
        } catch (Exception ex) {
            if (!ex.getMessage().equals("registerResourceEntry: cannot register resource " + ldri + " resource exists")) {
                fail(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        }

        //Add children to that resource
        String childLdri = "/child1";
        Path childPath = Path.path(ldri + childLdri);
        LogicalData child = new LogicalData(childPath);
        System.out.println("child: " + child.getUID() + " " + child.getLDRI());
        instance.registerResourceEntry(child);

        ILogicalData loadedChildEntry = instance.getResourceEntryByLDRI(childPath);
        boolean theSame = compareEntries(child, loadedChildEntry);
        assertTrue(theSame);

        System.out.println("Unregister: " + child.getLDRI() + " " + child.getUID());
        instance.unregisterResourceEntry(child);
        ILogicalData result = instance.getResourceEntryByLDRI(childPath);
        assertNull(result);

        instance.unregisterResourceEntry(parent);
        result = instance.getResourceEntryByLDRI(parentPath);
        assertNull(result);
    }

    private boolean compareEntries(ILogicalData entry, ILogicalData loadedEntry) {
        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
            if (entry.getUID().equals(loadedEntry.getUID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test of resourceEntryExists method, of class SimpleDRCatalogue.
     */
    @Test
    public void testResourceEntryExists() throws Exception {
        System.out.println("resourceEntryExists");
        String ldri = "/resource";
        Path path = Path.path(ldri);
        ILogicalData entry = new LogicalData(path);
        SimpleDRCatalogue instance = new SimpleDRCatalogue();

        Boolean expResult = false;
        Boolean result = instance.resourceEntryExists(entry);
        assertEquals(expResult, result);


        instance.registerResourceEntry(entry);
        expResult = true;
        result = instance.resourceEntryExists(entry);
        assertEquals(expResult, result);

        instance.unregisterResourceEntry(entry);
    }

    /**
     * Test of getTopLevelResourceEntries method, of class SimpleDRCatalogue.
     */
    @Test
    public void testGetTopLevelResourceEntries() throws Exception {
        System.out.println("getTopLevelResourceEntries");
        //        ArrayList<DataResourceEntry> topLevel = new ArrayList<DataResourceEntry>();
        LogicalData topEntry1 = new LogicalData(Path.path("/r1"));
        LogicalData topEntry2 = new LogicalData(Path.path("/r2"));
        LogicalData topEntry3 = new LogicalData(Path.path("/r3"));

        LogicalData entry11 = new LogicalData(Path.path("/r1/r11"));
        LogicalData entry21 = new LogicalData(Path.path("/r2/r21"));


        SimpleDRCatalogue instance = new SimpleDRCatalogue();
        instance.registerResourceEntry(topEntry1);
        instance.registerResourceEntry(topEntry2);
        instance.registerResourceEntry(topEntry3);
        instance.registerResourceEntry(entry11);
        instance.registerResourceEntry(entry21);

        Collection<ILogicalData> result = instance.getTopLevelResourceEntries();
        assertEquals(result.size(), 3);

        for (ILogicalData e : result) {
            if (e.getLDRI().getLength() > 1) {
                fail("Resource " + e.getLDRI() + " is not a top level");
            }
        }

        instance.unregisterResourceEntry(entry21);
        instance.unregisterResourceEntry(entry11);
        instance.unregisterResourceEntry(topEntry3);
        instance.unregisterResourceEntry(topEntry2);
        instance.unregisterResourceEntry(topEntry1);

        result = instance.getTopLevelResourceEntries();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testRenameEntry() throws Exception {
        System.out.println("testRenameEntry");
        SimpleDRCatalogue instance = new SimpleDRCatalogue();
        Path originalPath = Path.path("/oldResourceName");
        LogicalData e = new LogicalData(originalPath);
        
        instance.registerResourceEntry(e);
        Path newPath = Path.path("/newResourceName");
        instance.renameEntry(originalPath, newPath );
        ILogicalData loaded = instance.getResourceEntryByLDRI(newPath);

        assertNotNull(loaded);

        assertEquals(newPath.toString(), loaded.getLDRI().toString());

        instance.unregisterResourceEntry(loaded);
        
    }
}

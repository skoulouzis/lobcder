/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import com.bradmcevoy.common.Path;
import nl.uva.cs.lobcder.resources.DataResourceEntry;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import org.globus.util.TestUtil;
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
        IDataResourceEntry entry = new DataResourceEntry(path);

        SimpleDRCatalogue instance = new SimpleDRCatalogue();
        try {

            instance.registerResourceEntry(entry);
            System.out.println("entry:          " + entry.getLDRI());
        } catch (Exception ex) {
            if (!ex.getMessage().equals("mkdir: cannot register resource " + ldri + " resource exists")) {
                fail(ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }

        IDataResourceEntry loadedEntry = instance.getResourceEntryByLDRI(path);
        assertNotNull(loadedEntry);

//        System.out.println("entry:          " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getLDRI());

        boolean theSame = compareEntries(entry, loadedEntry);

        assertTrue(theSame);

        instance.unregisterResourceEntry(entry);

        //Add children to that resource
//        path = Path.path("/resource1/child1");
//        entry = new DataResourceEntry(path);
//        instance.registerResourceEntry(entry);
//        loadedEntry = instance.getResourceEntryByLDRI(path);
//
//        instance.printFSTree();
    }

    private boolean compareEntries(IDataResourceEntry entry, IDataResourceEntry loadedEntry) {
//        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
            if (entry.getUID().equals(loadedEntry.getUID())) {
                return true;
            }
        }
        return false;
    }
    /**
     * Test of getResourceEntryByLDRI method, of class SimpleDRCatalogue.
     */
//    @Test
//    public void testGetResourceEntryByLDRI() throws Exception {
//        System.out.println("getResourceEntryByLDRI");
//        Path logicalResourceName = null;
//        SimpleDRCatalogue instance = new SimpleDRCatalogue();
//        IDataResourceEntry expResult = null;
//        IDataResourceEntry result = instance.getResourceEntryByLDRI(logicalResourceName);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of unregisterResourceEntry method, of class SimpleDRCatalogue.
//     */
//    @Test
//    public void testUnregisterResourceEntry() throws Exception {
//        System.out.println("unregisterResourceEntry");
//        IDataResourceEntry entry = null;
//        SimpleDRCatalogue instance = new SimpleDRCatalogue();
//        instance.unregisterResourceEntry(entry);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of resourceEntryExists method, of class SimpleDRCatalogue.
//     */
//    @Test
//    public void testResourceEntryExists() throws Exception {
//        System.out.println("resourceEntryExists");
//        IDataResourceEntry entry = null;
//        SimpleDRCatalogue instance = new SimpleDRCatalogue();
//        Boolean expResult = null;
//        Boolean result = instance.resourceEntryExists(entry);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRoot method, of class SimpleDRCatalogue.
//     */
//    @Test
//    public void testGetRoot() throws Exception {
//        System.out.println("getRoot");
//        SimpleDRCatalogue instance = new SimpleDRCatalogue();
//        IDataResourceEntry expResult = null;
//        IDataResourceEntry result = instance.getRoot();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTopLevelResourceEntries method, of class SimpleDRCatalogue.
//     */
//    @Test
//    public void testGetTopLevelResourceEntries() throws Exception {
//        System.out.println("getTopLevelResourceEntries");
//        SimpleDRCatalogue instance = new SimpleDRCatalogue();
//        List expResult = null;
//        List result = instance.getTopLevelResourceEntries();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}

    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

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

        SimpleDLCatalogue instance = new SimpleDLCatalogue();
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

        SimpleDLCatalogue instance = new SimpleDLCatalogue();
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

    /**
     * Test of resourceEntryExists method, of class SimpleDRCatalogue.
     */
    @Test
    public void testResourceEntryExists() throws Exception {
        System.out.println("resourceEntryExists");
        String ldri = "/resource";
        Path path = Path.path(ldri);
        ILogicalData entry = new LogicalData(path);
        SimpleDLCatalogue instance = new SimpleDLCatalogue();

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

            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
    }

    @Test
    public void testRenameEntry() {
        System.out.println("testRenameEntry");
        SimpleDLCatalogue instance = null;
        ILogicalData loaded = null;
        try {

            instance = new SimpleDLCatalogue();
            Path originalPath = Path.path("/oldResourceName");
            LogicalData e = new LogicalData(originalPath);

            instance.registerResourceEntry(e);
            Path newPath = Path.path("/newResourceName");
            instance.renameEntry(originalPath, newPath);
            loaded = instance.getResourceEntryByLDRI(newPath);

            assertNotNull(loaded);

            assertEquals(newPath.toString(), loaded.getLDRI().toString());


        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);
            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }

    }

    @Test
    public void testMetadataPersistence() {
        System.out.println("testMetadataPersistence");
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        Path originalPath = Path.path("/oldResourceName");
        LogicalData e = new LogicalData(originalPath);
        Metadata meta = new Metadata();
        String type = "text/plain";
        meta.addContentType(type);
        meta.setCreateDate(System.currentTimeMillis());
        meta.setLength(new Long(0));
        e.setMetadata(meta);

        try {
            instance.registerResourceEntry(e);


            SimpleDLCatalogue instance2 = new SimpleDLCatalogue();
            ILogicalData entry = instance2.getResourceEntryByLDRI(originalPath);
            meta = entry.getMetadata();

            assertNotNull(meta);

            assertNotNull(meta.getContentTypes());

            assertEquals(type, meta.getContentTypes().get(0));

            assertNotNull(meta.getCreateDate());

            assertNotNull(meta.getLength());

            instance2.unregisterResourceEntry(entry);
        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                new SimpleDLCatalogue().unregisterResourceEntry(e);
            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
    }

    @Test
    public void testRegisterToNonExistiongParent() {
        System.out.println("testRegisterToNonExistiongParent");
        SimpleDLCatalogue instance = null;
        LogicalData lChild = null;
        try {

            instance = new SimpleDLCatalogue();
            Path parentPath = Path.path("parent");
            Path childPath = Path.path("parent/child");


            LogicalData lParent = new LogicalFolder(parentPath);
            lChild = new LogicalFile(childPath);

            instance.registerResourceEntry(lChild);

            fail("Should throw NonExistingResourceException");

        } catch (Exception ex) {
            if (ex instanceof DuplicateResourceException) {
                fail("Resource should not be registered: " + ex.getMessage());
            } else if (ex instanceof NonExistingResourceException) {
                //Test passed
            } else {
                fail("Not expected Exception: " + ex.getMessage());
            }
        } finally {
            try {
                instance.unregisterResourceEntry(lChild);
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }

        }
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
               if(lChild!=null)
                    new SimpleDLCatalogue().unregisterResourceEntry(lChild);       
                if(lParent!=null)
                    new SimpleDLCatalogue().unregisterResourceEntry(lParent);
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
        }
    }
    
    
        @Test
    public void testRegisterWithStorageSite() {
        System.out.println("testRegisterWithStorageSite");
        SimpleDLCatalogue instance = null;
        ILogicalData lParent = null;
        try {

            instance = new SimpleDLCatalogue();
            Path parentPath = Path.path("parent");

            lParent = new LogicalFolder(parentPath);
            ArrayList<StorageSite> sites = new ArrayList<StorageSite>();
            sites.add(new StorageSite("file:///tmp", new Credential("user1")));
            lParent.setStorageSites(sites);
            ArrayList<StorageSite> theSites = lParent.getStorageSites();
            
            assertNotNull(theSites);
            assertFalse(theSites.isEmpty());
            
            //When registering the rentry, the storage site is set to null
            instance.registerResourceEntry(lParent);
            lParent = instance.getResourceEntryByLDRI(parentPath);
                    
            theSites = lParent.getStorageSites();
            
            assertNotNull(theSites);
            assertFalse(theSites.isEmpty());

        } catch (Exception ex) {
            fail("Exception: " + ex.getMessage());
        } finally {
            try {
                if(lParent!=null)
                    new SimpleDLCatalogue().unregisterResourceEntry(lParent);
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
        }
    }

    private boolean compareEntries(ILogicalData entry, ILogicalData loadedEntry) {
//        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
            if (entry.getUID().equals(loadedEntry.getUID())) {
                return true;
            }
        }
        return false;
    }
}

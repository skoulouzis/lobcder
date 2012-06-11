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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import nl.uva.cs.lobcder.util.PropertiesLoader;
import nl.uva.cs.lobcder.webDav.resources.UserThreadRDBMS;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author S. Koulouzis
 */
public class RDMSDLCatalogueTest {

//    private static String[] names = new String[]{"storage1.prop", "storage2.prop", "storage3.prop"};
//    private static String[] names = new String[]{"storage1.prop"};
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
        ILogicalData entry = new LogicalData(path, Constants.LOGICAL_DATA);

        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        try {
//            StorageSite s = new StorageSite("", new Credential(""));
            Collection<IStorageSite> ss = new ArrayList<IStorageSite>();
            entry.setStorageSites(ss);
            instance.registerResourceEntry(entry);
            System.out.println("entry:          " + entry.getLDRI());

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

    @Test
    public void testUnregisterResourceEntry() throws Exception {
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));

        String ldri = "/resource1";
        Path path = Path.path(ldri);
        ILogicalData resource1 = new LogicalData(path, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(resource1);

        ldri = "/resource1/r2";
        path = Path.path(ldri);
        ILogicalData r2 = new LogicalData(path, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(r2);

        ldri = "/resource1/r2/r3";
        path = Path.path(ldri);
        ILogicalData r3 = new LogicalData(path, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(r3);

        ldri = "/resource1/r2/r4";
        path = Path.path(ldri);
        ILogicalData r4 = new LogicalData(path, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(r4);

        ldri = "/resource1/r2/r4/r5";
        path = Path.path(ldri);
        ILogicalData r5 = new LogicalData(path, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(r5);

        instance.unregisterResourceEntry(r2);

        assertNotNull(instance.getResourceEntryByLDRI(resource1.getLDRI()));
        assertNull(instance.getResourceEntryByLDRI(r5.getLDRI()));
        assertNull(instance.getResourceEntryByLDRI(r4.getLDRI()));
        assertNull(instance.getResourceEntryByLDRI(r3.getLDRI()));
        assertNull(instance.getResourceEntryByLDRI(r2.getLDRI()));

        instance.unregisterResourceEntry(resource1);
        assertNull(instance.getResourceEntryByLDRI(resource1.getLDRI()));
    }

    @Test
    public void testRegisterMultipleResourceEntry() throws Exception {
        System.out.println("testRegisterMultipleResourceEntry");
        String ldri = "/resource2";
        Path parentPath = Path.path(ldri);
        ILogicalData parent = new LogicalData(parentPath, Constants.LOGICAL_DATA);

        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));

        instance.registerResourceEntry(parent);

        //Add children to that resource
        String childLdri = "/child1";
        Path childPath = Path.path(ldri + childLdri);

        LogicalData child = new LogicalData(childPath, Constants.LOGICAL_DATA);
        instance.registerResourceEntry(child);

        ILogicalData loadedChildEntry = instance.getResourceEntryByLDRI(childPath);
        boolean theSame = compareEntries(child, loadedChildEntry);
        assertTrue(theSame);

//        System.out.println("Unregister: " + child.getLDRI() + " " + child.getUID());
//        instance.unregisterResourceEntry(child);
//        ILogicalData result = instance.getResourceEntryByLDRI(childPath);
//        assertNull(result);

        instance.unregisterResourceEntry(parent);
        ILogicalData result = instance.getResourceEntryByLDRI(parentPath);
        assertNull(result);

        result = instance.getResourceEntryByLDRI(childPath);
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
        ILogicalData entry = new LogicalData(path, Constants.LOGICAL_DATA);
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));

        Boolean expResult = false;
        Boolean result = instance.resourceEntryExists(entry);
        assertEquals(expResult, result);


        instance.registerResourceEntry(entry);
        expResult = true;
        result = instance.resourceEntryExists(entry);
        assertEquals(expResult, result);

        instance.unregisterResourceEntry(entry);
        ILogicalData loaded = instance.getResourceEntryByLDRI(path);
        assertNull(loaded);
    }

    /**
     * Test of getTopLevelResourceEntries method, of class SimpleDRCatalogue.
     */
    @Test
    public void testGetTopLevelResourceEntries() {
        System.out.println("getTopLevelResourceEntries");
        Collection<ILogicalData> topEntries = new ArrayList<ILogicalData>();
        LogicalData topEntry1 = new LogicalData(Path.path("/r1"), Constants.LOGICAL_DATA);
        topEntries.add(topEntry1);
        LogicalData topEntry2 = new LogicalData(Path.path("/r2"), Constants.LOGICAL_DATA);
        topEntries.add(topEntry2);
        LogicalData topEntry3 = new LogicalData(Path.path("/r3"), Constants.LOGICAL_DATA);
        topEntries.add(topEntry3);

        LogicalData entry11 = new LogicalData(Path.path("/r1/r11"), Constants.LOGICAL_DATA);
        LogicalData entry21 = new LogicalData(Path.path("/r2/r21"), Constants.LOGICAL_DATA);
        RDMSDLCatalog instance = null;
        Collection<ILogicalData> result = null;
        ILogicalData loaded;
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            instance.registerResourceEntry(topEntry1);
            instance.registerResourceEntry(topEntry2);
            instance.registerResourceEntry(topEntry3);
            instance.registerResourceEntry(entry11);
            instance.registerResourceEntry(entry21);

            result = instance.getTopLevelResourceEntries();

            boolean[] found = new boolean[]{false, false, false};
            for (ILogicalData d : result) {
                System.out.println("TOP:        " + d.getLDRI() + "     " + d.getUID());

                if (compareEntries(d, topEntry3)) {
                    found[0] = true;
                }

                if (compareEntries(d, topEntry2)) {
                    found[1] = true;
                }

                if (compareEntries(d, topEntry1)) {
                    found[2] = true;
                }
//                if (!compareEntries(d, topEntry3)) {
//                    if (!compareEntries(d, topEntry2)) {
//                        if (!compareEntries(d, topEntry1)) {
//                            fail("Resource " + topEntry1.getLDRI() + " is not returned by query!");
//                        }
//                    }
//                }
            }

            for (boolean b : found) {
                assertTrue(b);
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

    @Test
    public void testRenameEntry() {
        System.out.println("testRenameEntry");
        RDMSDLCatalog instance = null;
        ILogicalData loaded = null;
        Path newPath = null;
        LogicalData parent =null;
        
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path originalParentPath = Path.path("/ResourceName/");
            Path originalChildPath = Path.path("/ResourceName/oldResourceName");
            parent = new LogicalData(originalParentPath, Constants.LOGICAL_DATA);
            LogicalData child = new LogicalData(originalChildPath, Constants.LOGICAL_DATA);

            instance.registerResourceEntry(parent);
            instance.registerResourceEntry(child);
            newPath = Path.path("/ResourceName/newResourceName");

            instance.renameEntry(originalChildPath, newPath);

            loaded = instance.getResourceEntryByLDRI(newPath);
            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());
            
            ILogicalData loadedOriginal = instance.getResourceEntryByLDRI(originalChildPath);
            assertNull(loadedOriginal);
            
            
        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);
                loaded = instance.getResourceEntryByLDRI(newPath);
                assertNull(loaded);
                instance.unregisterResourceEntry(parent);
                assertNull(instance.getResourceEntryByLDRI(parent.getLDRI()));

            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
    }

    @Test
    public void testRenameWithChildren() {
        System.out.println("testRenameWithChildren");
        RDMSDLCatalog instance = null;
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

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path originalPath = Path.path("/oldResourceName/");
            LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(e);

            Path originalChildPath1 = Path.path("/oldResourceName/" + childName1);
            childEntry1 = new LogicalData(originalChildPath1, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(childEntry1);

            Path originalChildPath2 = Path.path("/oldResourceName/" + childName2);
            childEntry2 = new LogicalData(originalChildPath2, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(childEntry2);

            newPath = Path.path("/newResourceName");
            instance.renameEntry(originalPath, newPath);


            loaded = instance.getResourceEntryByLDRI(newPath);

            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());

            Collection<String> children = loaded.getChildren();
            assertNotNull(children);
            assertFalse(children.isEmpty());

            for (String p : children) {
                if (p.equals(Path.path(newPath, childName1).toString()) || p.equals(Path.path(newPath, childName2).toString())) {
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

    @Test
    public void testRenameWithManyChildren() {
        System.out.println("testRenameWithChildren");
        RDMSDLCatalog instance = null;
        try {
            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path originalPath = Path.path("/testCollection/");
            LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_FOLDER);
            instance.registerResourceEntry(e);

            Path originalChildPath1 = Path.path("/testCollection/childName1");
            LogicalData childEntry1 = new LogicalData(originalChildPath1, Constants.LOGICAL_FOLDER);
            instance.registerResourceEntry(childEntry1);
            String sub1 = "sub1";

            Path originalChildSubPath1 = Path.path("/testCollection/childName1/" + sub1);
            LogicalData childSubEntry1 = new LogicalData(originalChildSubPath1, Constants.LOGICAL_FILE);
            instance.registerResourceEntry(childSubEntry1);

            String sub2 = "sub2";
            Path originalChildSubPath2 = Path.path("/testCollection/childName1/" + sub2);
            LogicalData childSubEntry2 = new LogicalData(originalChildSubPath2, Constants.LOGICAL_FILE);
            instance.registerResourceEntry(childSubEntry2);
            
            Path newPath = Path.path("/testCollection/NewChildName1/");
            instance.renameEntry(originalChildPath1, newPath);

            ILogicalData loaded = instance.getResourceEntryByLDRI(newPath);
            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());

            assertNull(instance.getResourceEntryByLDRI(originalChildPath1));

            Collection<String> children = loaded.getChildren();
            assertNotNull(children);
            assertFalse(children.isEmpty());

            int foundIt = 0;
            Path newSubPath1 = Path.path(newPath, sub1);
            Path newSubPath2 = Path.path(newPath, sub2);
            for (String p : children) {
                if (p.equals(newSubPath1.toString()) || p.equals(newSubPath2.toString())) {
                    foundIt++;
                }
            }
            assertEquals(foundIt, 2);
            ILogicalData oldSubEntry1 = instance.getResourceEntryByLDRI(originalChildSubPath1);
            assertNull(oldSubEntry1);
            ILogicalData oldSubEntry2 = instance.getResourceEntryByLDRI(originalChildSubPath2);
            assertNull(oldSubEntry2);

            ILogicalData newSubEntry1 = instance.getResourceEntryByLDRI(newSubPath1);
            assertNotNull(newSubEntry1);
            ILogicalData newSubEntry2 = instance.getResourceEntryByLDRI(newSubPath2);
            assertNotNull(newSubEntry2);


            loaded = instance.getResourceEntryByLDRI(originalPath);
            assertNotNull(loaded.getChild(newPath));
            assertNull(loaded.getChild(originalChildPath1));


            instance.unregisterResourceEntry(e);
            loaded = instance.getResourceEntryByLDRI(originalPath);
            assertNull(loaded);


            newSubEntry1 = instance.getResourceEntryByLDRI(newSubPath1);
            assertNull(newSubEntry1);
            newSubEntry2 = instance.getResourceEntryByLDRI(newSubPath2);
            assertNull(newSubEntry2);

        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        }

    }

    @Test
    public void testRenameChild2() throws Exception {
        RDMSDLCatalog instance = null;
        Path tesCollectionPath = null;
        ILogicalData logicalCollection = null;
        ILogicalData logicalChild = null;
        Path testNewChildPath = null;
        ILogicalData loadedRenamedLogicalChild = null;
        try {
            System.out.println("testRenameChild2");

            tesCollectionPath = Path.path("/testCollection");
            logicalCollection = new LogicalData(tesCollectionPath, Constants.LOGICAL_FOLDER);
            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            instance.registerResourceEntry(logicalCollection);
            //Check if the collection is registered
            ILogicalData loadedLogicalCollection = instance.getResourceEntryByLDRI(tesCollectionPath);
            assertNotNull(loadedLogicalCollection);
            compareEntries(loadedLogicalCollection, logicalCollection);

            Path testChildPath = Path.path(tesCollectionPath.toString() + "/testChild");
            logicalChild = new LogicalData(testChildPath, Constants.LOGICAL_FILE);
            instance.registerResourceEntry(logicalChild);
            //Check if the child is registered
            ILogicalData loadedLogicalChild = instance.getResourceEntryByLDRI(testChildPath);
            assertNotNull(loadedLogicalChild);
            compareEntries(loadedLogicalChild, logicalChild);

            //Check if collection has the child 
            loadedLogicalCollection = instance.getResourceEntryByLDRI(tesCollectionPath);
            Path loadedChildPath = loadedLogicalCollection.getChild(testChildPath);
            assertEquals(loadedChildPath.toString(), testChildPath.toString());

            //Rename child 
            testNewChildPath = Path.path(tesCollectionPath.toString() + "/testNewChild");
            instance.renameEntry(testChildPath, testNewChildPath);
            loadedRenamedLogicalChild = instance.getResourceEntryByLDRI(testNewChildPath);
            assertNotNull(loadedRenamedLogicalChild);

            loadedLogicalChild = instance.getResourceEntryByLDRI(testChildPath);
            assertNull(loadedLogicalChild);
            //check if collection has renamed child
            ILogicalData loadedCollection = instance.getResourceEntryByLDRI(logicalCollection.getLDRI());
            Path loadedNewChild = loadedCollection.getChild(testNewChildPath);
            assertNotNull(loadedNewChild);


        } catch (CatalogueException ex) {
            fail(ex.getMessage());
            Logger.getLogger(RDMSDLCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {

                instance.unregisterResourceEntry(logicalCollection);
                ILogicalData loadedCollection = instance.getResourceEntryByLDRI(tesCollectionPath);
                assertNull(loadedCollection);

                ILogicalData loadedChild = instance.getResourceEntryByLDRI(logicalChild.getLDRI());
                assertNull(loadedChild);

                loadedChild = instance.getResourceEntryByLDRI(loadedRenamedLogicalChild.getLDRI());
                assertNull(loadedChild);

            } catch (CatalogueException ex) {
                fail(ex.getMessage());
            }
        }
    }

    @Test
    public void testRenameChild() {
        System.out.println("testRenameChild");
        RDMSDLCatalog instance = null;
        ILogicalData loaded = null;
        String childName1 = "Child1";
        String childName2 = "Child2";

        ILogicalData childEntry1 = null;
        ILogicalData childEntry2 = null;
        Path newPath = null;
        ILogicalData childEntry2Loaded;
        ILogicalData parent = null;
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path originalPath = Path.path("/oldResourceName/");
            parent = new LogicalData(originalPath, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(parent);

            Path originalChildPath1 = Path.path("/oldResourceName/" + childName1);
            childEntry1 = new LogicalData(originalChildPath1, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(childEntry1);

            Path originalChildPath2 = Path.path("/oldResourceName/" + childName1 + "/" + childName2);
            childEntry2 = new LogicalData(originalChildPath2, Constants.LOGICAL_DATA);
            instance.registerResourceEntry(childEntry2);

            newPath = Path.path("/oldResourceName/" + childName1 + "/newChild2");
            instance.renameEntry(originalChildPath2, newPath);

            loaded = instance.getResourceEntryByLDRI(newPath);

            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());

        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);
                childEntry2Loaded = instance.getResourceEntryByLDRI(loaded.getLDRI());
                assertNull(childEntry2Loaded);

                instance.unregisterResourceEntry(childEntry1);
                childEntry1 = instance.getResourceEntryByLDRI(childEntry1.getLDRI());
                assertNull(childEntry1);

                instance.unregisterResourceEntry(parent);
                parent = instance.getResourceEntryByLDRI(parent.getLDRI());
                assertNull(parent);
            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
    }

    @Test
    public void testMetadataPersistence() {
        System.out.println("testMetadataPersistence");
        RDMSDLCatalog instance = null;
        Path originalPath = Path.path("/oldResourceName");
        LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_DATA);
        Metadata meta = new Metadata();
        String type = "text/plain";
        meta.addContentType(type);
        meta.setCreateDate(System.currentTimeMillis());
        meta.setLength(new Long(0));
        e.setMetadata(meta);

        try {
            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            instance.registerResourceEntry(e);


            RDMSDLCatalog instance2 = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
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
        }
    }
    
    
    
     @Test
    public void testPermissions() {
        RDMSDLCatalog instance = null;
        Path originalPath = Path.path("/resource");
        LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_DATA);
        Metadata meta = new Metadata();
        ArrayList<Integer> permArray1 = new ArrayList<Integer>();
        permArray1.add(0);
        permArray1.add(1);
        permArray1.add(5);
        meta.setPermissionArray(permArray1);
        e.setMetadata(meta);

        try {
            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            instance.registerResourceEntry(e);


            ILogicalData entry = instance.getResourceEntryByLDRI(originalPath);
            meta = entry.getMetadata();

            assertNotNull(meta);
            List<Integer> permArray2 = meta.getPermissionArray();
            assertNotNull(permArray2);
            assertEquals(permArray1,permArray2);

            instance.unregisterResourceEntry(entry);
        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
        }
    }

    @Test
    public void testRegisterToNonExistiongParent() {
        System.out.println("testRegisterToNonExistiongParent");
        RDMSDLCatalog instance = null;
        LogicalData lChild = null;
        ILogicalData res;
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path parentPath = Path.path("parent");
            Path childPath = Path.path("parent/child");

            LogicalData lParent = new LogicalData(parentPath, Constants.LOGICAL_FOLDER);
            lChild = new LogicalData(childPath, Constants.LOGICAL_FILE);

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
                //Since we got an exeption the entry should not be there 
                res = instance.getResourceEntryByLDRI(lChild.getLDRI());
                assertNull(res);
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }

        }
    }

    @Test
    public void testRegisterToExistiongParent() {
        System.out.println("testRegisterToExistiongParent");
        RDMSDLCatalog instance = null;
        LogicalData lChild = null;
        LogicalData lParent = null;
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path parentPath = Path.path("parent");
            Path childPath = Path.path("parent/child");

            lParent = new LogicalData(parentPath, Constants.LOGICAL_FOLDER);
            lChild = new LogicalData(childPath, Constants.LOGICAL_FILE);

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
                    new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties")).unregisterResourceEntry(lChild);
                }
                if (lParent != null) {
                    new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties")).unregisterResourceEntry(lParent);
                }
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
        }
    }

    @Test
    public void testRegisterWithStorageSite() {
        System.out.println("testRegisterWithStorageSite");
        RDMSDLCatalog instance = null;
        ILogicalData lParent = null;
        try {

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Path parentPath = Path.path("parent");

            lParent = new LogicalData(parentPath, Constants.LOGICAL_FOLDER);
            ArrayList<IStorageSite> sites = new ArrayList<IStorageSite>();
            sites.add(new StorageSite("file:///tmp", new Credential("user1".split(","))));
            lParent.setStorageSites(sites);
            Collection<IStorageSite> theSites = lParent.getStorageSites();

            assertNotNull(theSites);
            assertFalse(theSites.isEmpty());

            //When registering the entry, the storage site is set to null
            instance.registerResourceEntry(lParent);
            lParent = instance.getResourceEntryByLDRI(parentPath);

            theSites = lParent.getStorageSites();

            assertNotNull(theSites);
            assertFalse(theSites.isEmpty());

        } catch (Exception ex) {
            fail("Exception: " + ex.getMessage());
        } finally {
            try {
                if (lParent != null) {
                    new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties")).unregisterResourceEntry(lParent);
                }
            } catch (Exception ex) {
                fail("Exception: " + ex.getMessage());
            }
        }
    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetSitesByUnames() throws Exception {
        System.out.println("testGetSitesByUnames");
        populateStorageSites();
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        try {
            String uname = "uname2";
            Properties prop = new Properties();
            prop.setProperty(Constants.VPH_USERNAMES, uname);
            prop.setProperty(Constants.STORAGE_SITE_USERNAME, "non");
            prop.setProperty(Constants.STORAGE_SITE_PASSWORD, "non");
            prop.setProperty(Constants.STORAGE_SITE_ENDPOINT, "file:///" + System.getProperty("user.home") + "/deleteMe/");

            instance.registerStorageSite(prop);
            boolean exists = instance.storageSiteExists(prop);
            assertTrue(exists);

            Collection<IStorageSite> result = instance.getSitesByUname(uname);
            assertNotNull(result);
            assertFalse(result.isEmpty());

            for (IStorageSite s : result) {
                assertTrue(s.getVPHUsernames().contains(uname));
            }

        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    @Test
    public void testUpdateResourceEntry() {
        RDMSDLCatalog instance = null;
        ILogicalData loaded = null;
        try {
            System.out.println("testUpdateResourceEntry");

            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            LogicalData newEntry = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_1, Constants.LOGICAL_FILE);
            instance.registerResourceEntry(newEntry);
            loaded = instance.getResourceEntryByLDRI(newEntry.getLDRI());
            boolean same = compareEntries(newEntry, loaded);
            assertTrue(same);

            Metadata meta = newEntry.getMetadata();
            String mime = "application/octet-stream";
            meta.addContentType(mime);
            long create = System.currentTimeMillis();
            meta.setCreateDate(create);
            Long len = new Long(32);
            meta.setLength(len);
            long mod = System.currentTimeMillis();
            meta.setModifiedDate(mod);
            newEntry.setMetadata(meta);


            Collection<String> children = new CopyOnWriteArrayList<String>();
            children.add(ConstantsAndSettings.TEST_FILE_PATH_2.toString());
            newEntry.setChildren(children);

//            ArrayList<IStorageSite> sites = new ArrayList<IStorageSite>();
//            sites.add(new StorageSite("file:///tmp", new Credential("user1")));
//            newEntry.setStorageSites(sites);

            instance.updateResourceEntry(newEntry);
            loaded = instance.getResourceEntryByLDRI(newEntry.getLDRI());
            same = compareEntries(newEntry, loaded);
            assertTrue(same);

            Metadata loadedMeta = loaded.getMetadata();
            assertTrue(loadedMeta.getContentTypes().contains(mime));
            assertEquals(loadedMeta.getCreateDate(), new Long(create));
            assertEquals(loadedMeta.getModifiedDate(), new Long(mod));
            assertEquals(loadedMeta.getLength(), len);

        } catch (Exception ex) {
            fail();
            Logger.getLogger(RDMSDLCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);

            } catch (CatalogueException ex) {
                Logger.getLogger(RDMSDLCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Test
    public void testStorageSiteExists() {
        RDMSDLCatalog instance = null;
        try {
            instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            String uname = "uname2";
            Properties prop = new Properties();
            prop.setProperty(Constants.VPH_USERNAMES, uname);
            prop.setProperty(Constants.STORAGE_SITE_USERNAME, "non");
            prop.setProperty(Constants.STORAGE_SITE_PASSWORD, "non");
            prop.setProperty(Constants.STORAGE_SITE_ENDPOINT, "file:///" + System.getProperty("user.home") + "/deleteMe/");

            instance.registerStorageSite(prop);
            boolean exists = instance.storageSiteExists(prop);
            assertTrue(exists);

            Collection<IStorageSite> result = instance.getSitesByUname(uname);
            assertNotNull(result);
            assertFalse(result.isEmpty());

            for (IStorageSite s : result) {
                assertTrue(s.getVPHUsernames().contains(uname));
            }
        } catch (CatalogueException ex) {
            Logger.getLogger(RDMSDLCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    @Test
    public void testCreateFileDeleteItAndGetParent() throws FileNotFoundException, IOException, CatalogueException, Exception {
        //The no such row bug. For some reason when we delete the file and then try 
        //to get the parent we get  No such database row exception for the 
        //StorageSite which makes the LogicalData not to detach properly
        RDMSDLCatalog catalogue = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        //Init with some storage sites
        //        String propBasePath = System.getProperty("user.home") + File.separator
        //                + "workspace" + File.separator + "lobcder"
        //                + File.separator + "etc" + File.separator;
        Properties[] props = PropertiesLoader.getStorageSitesProps();
//        ArrayList<String> endpoints = new ArrayList<String>();

        Properties prop = null;
        for (Properties p : props) {
//            prop = getCloudProperties(propBasePath + name);
//            endpoints.add(prop.getProperty(Constants.STORAGE_SITE_ENDPOINT));
            catalogue.registerStorageSite(p);
            prop = p;
        }

        //Create root
        LogicalData root = new LogicalData(Path.root, Constants.LOGICAL_FOLDER);
        Collection<IStorageSite> sites;
        sites = root.getStorageSites();
        if (sites == null || sites.isEmpty()) {
            sites = (Collection<IStorageSite>) catalogue.getSitesByUname(prop.getProperty(Constants.VPH_USERNAMES));
            root.setStorageSites(sites);
            catalogue.registerResourceEntry(root);
        }
        assertNotNull(catalogue.getResourceEntryByLDRI(Path.root));



        //Test collection 
        Path newCollectionPath = Path.path(Path.root, ConstantsAndSettings.TEST_FOLDER_NAME_1);
        LogicalData newFolderEntry = new LogicalData(newCollectionPath, Constants.LOGICAL_FOLDER);
        newFolderEntry.getMetadata().setCreateDate(System.currentTimeMillis());
        sites = root.getStorageSites();
        newFolderEntry.setStorageSites(sites);
        catalogue.registerResourceEntry(newFolderEntry);
        assertNotNull(catalogue.getResourceEntryByLDRI(newCollectionPath));

        //Test file 
        Path newFilePath = Path.path(newCollectionPath, ConstantsAndSettings.TEST_FILE_NAME_1);
        LogicalData newFileResource = (LogicalData) catalogue.getResourceEntryByLDRI(newFilePath);
        if (newFileResource == null) {
            newFileResource = new LogicalData(newFilePath, Constants.LOGICAL_FILE);
            sites = newFolderEntry.getStorageSites();
            newFileResource.setStorageSites(sites);
            catalogue.registerResourceEntry(newFileResource);
        }
        assertNotNull(catalogue.getResourceEntryByLDRI(newFilePath));

        //delete file 
        catalogue.unregisterResourceEntry(newFileResource);
        assertNull(catalogue.getResourceEntryByLDRI(newFilePath));

        ILogicalData entry = catalogue.getResourceEntryByLDRI(newCollectionPath);
        assertNotNull(entry);

        catalogue.unregisterResourceEntry(entry);
        assertNull(catalogue.getResourceEntryByLDRI(newCollectionPath));

        catalogue.unregisterResourceEntry(root);
        assertNull(catalogue.getResourceEntryByLDRI(root.getLDRI()));

    }

    @Test
    public void testMultiThread() {
        try {
            System.out.println("testMultiThread");
            Thread userThread1 = new UserThreadRDBMS(2);
            userThread1.setName("T1");

            Thread userThread2 = new UserThreadRDBMS(2);
            userThread2.setName("T2");

            Thread userThread3 = new UserThreadRDBMS(3);
            userThread3.setName("T3");


            userThread1.start();
            int min = 1;
            int max = 100;
            int sleeptime = min + (int) (Math.random() * ((max - min) + 1));

            Thread.sleep(sleeptime);
            userThread2.start();

            sleeptime = min + (int) (Math.random() * ((max - min) + 1));

            Thread.sleep(sleeptime);
            userThread3.start();

            userThread1.join();
            userThread2.join();
            userThread3.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(RDMSDLCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void populateStorageSites() throws FileNotFoundException, IOException, Exception {
        RDMSDLCatalog instance = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
        instance.clearAllSites();
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder"
//                + File.separator + "etc" + File.separator;
//        ArrayList<String> endpoints = new ArrayList<String>();
        Properties[] props = PropertiesLoader.getStorageSitesProps();

        for (Properties p : props) {
//            Properties prop = getCloudProperties(propBasePath + name);
//            endpoints.add(prop.getProperty(Constants.STORAGE_SITE_ENDPOINT));
            instance.registerStorageSite(p);
            boolean exists = instance.storageSiteExists(p);
            assertTrue(exists);
        }
    }

    private boolean compareEntries(ILogicalData entry, ILogicalData loadedEntry) {
        if (entry.getLDRI().toString().equals(loadedEntry.getLDRI().toString()) && entry.getType().equals(loadedEntry.getType())) {
            if (entry.getUID().equals(loadedEntry.getUID())) {
                if (entry.getPDRI().toString().equals(loadedEntry.getPDRI().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}

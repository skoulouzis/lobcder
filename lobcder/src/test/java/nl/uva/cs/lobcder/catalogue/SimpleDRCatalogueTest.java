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
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import nl.uva.cs.lobcder.webDav.resources.Constants;
import nl.uva.cs.lobcder.webDav.resources.UserThread;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author S. Koulouzis
 */
public class SimpleDRCatalogueTest {

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
        ILogicalData entry = new LogicalData(path, Constants.LOGICAL_DATA);

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

    @Test
    public void testRegisterMultipleResourceEntry() throws Exception {
        System.out.println("testRegisterMultipleResourceEntry");
        String ldri = "/resource2";
        Path parentPath = Path.path(ldri);
        ILogicalData parent = new LogicalData(parentPath, Constants.LOGICAL_DATA);

        SimpleDLCatalogue instance = new SimpleDLCatalogue();

        instance.registerResourceEntry(parent);

        //Add children to that resource
        String childLdri = "/child1";
        Path childPath = Path.path(ldri + childLdri);

        LogicalData child = new LogicalData(childPath, Constants.LOGICAL_DATA);
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
        ILogicalData entry = new LogicalData(path, Constants.LOGICAL_DATA);
        SimpleDLCatalogue instance = new SimpleDLCatalogue();

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
        SimpleDLCatalogue instance = null;
        ILogicalData loaded = null;
        Path newPath = null;
        try {

            instance = new SimpleDLCatalogue();
            Path originalPath = Path.path("/oldResourceName");
            LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_DATA);

            instance.registerResourceEntry(e);
            newPath = Path.path("/newResourceName");

            instance.renameEntry(originalPath, newPath);

            loaded = instance.getResourceEntryByLDRI(newPath);
            assertNotNull(loaded);
            assertEquals(newPath.toString(), loaded.getLDRI().toString());


            ILogicalData loadedOriginal = instance.getResourceEntryByLDRI(originalPath);
            assertNull(loadedOriginal);


        } catch (Exception ex) {
            fail("Unexpected Exception: " + ex.getMessage());
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);
                loaded = instance.getResourceEntryByLDRI(newPath);
                assertNull(loaded);

            } catch (Exception ex) {
                fail("Unexpected Exception: " + ex.getMessage());
            }
        }
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

    @Test
    public void testRenameChild() {
        System.out.println("testRenameChild");
        SimpleDLCatalogue instance = null;
        ILogicalData loaded = null;
        String childName1 = "Child1";
        String childName2 = "Child2";

        ILogicalData childEntry1 = null;
        ILogicalData childEntry2 = null;
        Path newPath = null;
        ILogicalData childEntry2Loaded;
        ILogicalData parent = null;
        try {

            instance = new SimpleDLCatalogue();
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
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        Path originalPath = Path.path("/oldResourceName");
        LogicalData e = new LogicalData(originalPath, Constants.LOGICAL_DATA);
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
        ILogicalData res;
        try {

            instance = new SimpleDLCatalogue();
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
        SimpleDLCatalogue instance = null;
        LogicalData lChild = null;
        LogicalData lParent = null;
        try {

            instance = new SimpleDLCatalogue();
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

    @Test
    public void testRegisterWithStorageSite() {
        System.out.println("testRegisterWithStorageSite");
        SimpleDLCatalogue instance = null;
        ILogicalData lParent = null;
        try {

            instance = new SimpleDLCatalogue();
            Path parentPath = Path.path("parent");

            lParent = new LogicalData(parentPath, Constants.LOGICAL_FOLDER);
            ArrayList<IStorageSite> sites = new ArrayList<IStorageSite>();
            sites.add(new StorageSite("file:///tmp", new Credential("user1")));
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
                    new SimpleDLCatalogue().unregisterResourceEntry(lParent);
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
                assertEquals(s.getVPHUsername(), uname);
            }

        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    @Test
    public void testUpdateResourceEntry() {
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        ILogicalData loaded = null;
        try {
            System.out.println("testUpdateResourceEntry");


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


            Collection<Path> children = new ArrayList<Path>();
            children.add(ConstantsAndSettings.TEST_FILE_PATH_2);
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
            Logger.getLogger(SimpleDRCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.unregisterResourceEntry(loaded);

            } catch (CatalogueException ex) {
                Logger.getLogger(SimpleDRCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Test
    public void testStorageSiteExistsy() {
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
                assertEquals(s.getVPHUsername(), uname);
            }
        } catch (CatalogueException ex) {
            Logger.getLogger(SimpleDRCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            instance.clearAllSites();
            Collection<StorageSite> allSites = instance.getAllSites();
            assertEquals(allSites.size(), 0);
        }
    }

    @Test
    public void testMultiThread() {
        try {
            System.out.println("testMultiThread");
            Thread userThread1 = new UserThread(2);
            userThread1.setName("T1");

            Thread userThread2 = new UserThread(2);
            userThread2.setName("T2");


            userThread1.start();
            userThread2.start();

            userThread1.join();
            userThread2.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(SimpleDRCatalogueTest.class.getName()).log(Level.SEVERE, null, ex);
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
//        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName()) && entry.getType().equals(loadedEntry.getType())) {
//            if (entry.getUID().equals(loadedEntry.getUID())) {
            return true;
//            }
        }
        return false;
    }
}

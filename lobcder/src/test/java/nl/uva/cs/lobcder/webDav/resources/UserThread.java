/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author skoulouz
 */
public class UserThread extends Thread {

    private final int opNum;
    private static int counter = 0;

    public UserThread(int opNmu) {
        this.opNum = opNmu;
    }

    @Override
    public void run() {
        counter++;
        switch (opNum) {
            case 1:
                op1();
                break;
            case 2:
                op2();
                break;

            case 3:
                op3();
                break;
            default:
                op1();
                break;
        }
    }

    private void op1() {
        try {
            String host = "localhost:8080";
            String fileName = "testFileThread" + getName();//ConstantsAndSettings.TEST_FILE_NAME_1;
            String collectionName = "testCollection" + getName();

            WebDataResourceFactory instance = new WebDataResourceFactory();
            WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + collectionName);
            if (result == null) {
                WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
                assertNotNull(root);
                Collection<IStorageSite> sites = root.getStorageSites();
                assertFalse(sites.isEmpty());

                result = (WebDataDirResource) root.createCollection(collectionName);
            }

            assertNotNull(result);
            Collection<IStorageSite> sites = result.getStorageSites();
            assertFalse(sites.isEmpty());

            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            WebDataFileResource file = (WebDataFileResource) result.createNew(fileName, bais, new Long("DATA".getBytes().length), "text/plain");
            checkChildren(result, file);
            Long len = file.getContentLength();
            assertEquals(len, new Long("DATA".getBytes().length));
//
            String acceps = "text/html,text/*;q=0.9";
            String res = file.getContentType(acceps);
            String expResult = "text/*;q=0.9";
            assertEquals(expResult, res);

            Date date = file.getCreateDate();
            assertNotNull(date);
            date = file.getModifiedDate();
            assertNotNull(date);

            String name = file.getName();
            assertEquals(fileName, name);

            instance = new WebDataResourceFactory();
            result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + collectionName);
            assertNotNull(result);
            sites = result.getStorageSites();
            assertFalse(sites.isEmpty());


            bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            file = (WebDataFileResource) result.createNew(fileName, bais, new Long("DATA".getBytes().length), "text/plain");
            checkChildren(result, file);

//
//            len = file.getContentLength();
//            assertEquals(len, new Long("DATA".getBytes().length));
//
//            acceps = "text/html,text/*;q=0.9";
//            res = file.getContentType(acceps);
//            expResult = "text/*;q=0.9";
//            assertEquals(expResult, res);
//
//            date = file.getCreateDate();
//            assertNotNull(date);
//            date = file.getModifiedDate();
//            assertNotNull(date);
//
//
//            name = file.getName();
//            assertEquals(fileName, name);

            result.delete();
            result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + collectionName);
            assertNull(result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void op2() {
        try {
            testUpdateResourceEntry();
            testRegisterMultipleResourceEntry();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void op3() {
        try {
            testRegisterWithStorageSite();
            testRegisterMultipleResourceEntry();
            testUpdateResourceEntry();
//            RDMSDLCatalogueTest t = new RDMSDLCatalogueTest();
//            t.testGetTopLevelResourceEntries();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void checkChildren(WebDataDirResource result, WebDataFileResource file) {
        List<? extends Resource> children = result.getChildren();
        assertFalse(children.isEmpty());
        boolean foundIt = false;
        for (Resource r : children) {
            if (r.getUniqueId().equals(file.getUniqueId())) {
                foundIt = true;
            }
        }
        assertTrue(foundIt);
    }

    public void testUpdateResourceEntry() {
        RDMSDLCatalog instance = new RDMSDLCatalog();
        ILogicalData loaded = null;
        try {
            System.out.println("testUpdateResourceEntry");
            LogicalData newEntry = new LogicalData(Path.path("testFileThread"+getName()), Constants.LOGICAL_FILE);
            
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
            children.add(Path.path("child1").toString());
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
            ex.printStackTrace();
        } finally {
            try {
                if (loaded != null && instance.resourceEntryExists(loaded)) {
                    instance.unregisterResourceEntry(loaded);
                }

            } catch (CatalogueException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void testRegisterMultipleResourceEntry() throws Exception {
        System.out.println("testRegisterMultipleResourceEntry");
        String ldri = null;
        String childLdri = null;
        if (this.getName().equals("T1")) {
            ldri = "/resource1";
            childLdri = "/child1";
        } else if (this.getName().equals("T2")) {
            ldri = "/resource2";
            childLdri = "/child2";
        }
        Path parentPath = Path.path(ldri);
        ILogicalData parent = new LogicalData(parentPath, Constants.LOGICAL_DATA);

        RDMSDLCatalog instance = new RDMSDLCatalog();

        instance.registerResourceEntry(parent);

        //Add children to that resource

        Path childPath = Path.path(ldri + childLdri);

        LogicalData child = new LogicalData(childPath, Constants.LOGICAL_DATA);
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

    @Test
    public void testRegisterWithStorageSite() {
        System.out.println("testRegisterWithStorageSite");
        RDMSDLCatalog instance = null;
        ILogicalData lParent = null;
        try {

            instance = new RDMSDLCatalog();
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
                    new RDMSDLCatalog().unregisterResourceEntry(lParent);
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
            //Due to very bad ptaches the UDIs are not the same any more 
            if (entry.getUID().equals(loadedEntry.getUID())) {
                return true;
            }
        }
        return false;
    }
}

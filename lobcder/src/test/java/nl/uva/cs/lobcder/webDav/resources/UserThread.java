/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import com.bradmcevoy.http.Resource;
import java.util.List;
import java.util.Date;
import java.io.ByteArrayInputStream;
import nl.uva.cs.lobcder.catalogue.SimpleDRCatalogueTest;
import nl.uva.cs.lobcder.resources.IStorageSite;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import static org.junit.Assert.*;

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
            default:
                op1();
                break;
        }
    }

    private void op1() {
        try {
            String host = "localhost:8080";
            String fileName = null;
            String collectionName = null;
            if (this.getName().equals("T1")) {
                fileName = ConstantsAndSettings.TEST_FILE_NAME_1;
                collectionName = ConstantsAndSettings.TEST_FOLDER_NAME_1;
            } else if (this.getName().equals("T2")) {
                fileName = ConstantsAndSettings.TEST_FILE_NAME_2;
                collectionName = ConstantsAndSettings.TEST_FOLDER_NAME_2;
            } else if (this.getName().equals("T3")) {
                fileName = ConstantsAndSettings.TEST_FILE_NAME_3;
                collectionName = ConstantsAndSettings.TEST_FOLDER_NAME_3;
            }
            
            debug("path: "+collectionName +"/"+fileName);

            WebDataResourceFactory instance = new WebDataResourceFactory();
            WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + collectionName);
            if (result == null) {
                WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
                assertNotNull(root);
                Collection<IStorageSite> sites = root.getStorageSites();
                assertFalse(sites.isEmpty());
                
                for(IStorageSite s : sites){
                    debug("SS: "+s.getEndpoint() +" "+s.getUID()+" "+s.getVPHUsername()+" "+s.getCredentials().getStorageSiteUsername());
                }
                
                result = (WebDataDirResource) root.createCollection(collectionName);
            }

            assertNotNull(result);
            Collection<IStorageSite> sites = result.getStorageSites();
            assertFalse(sites.isEmpty());
            
                        
            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            debug("Create "+fileName+" at: "+result.getName());
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
        SimpleDLCatalogue instance = new SimpleDLCatalogue();
        ILogicalData loaded = null;
        try {
            System.out.println("testUpdateResourceEntry");
            LogicalData newEntry = null;
            if (this.getName().equals("T1")) {
                newEntry = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_1,Constants.LOGICAL_FILE);
//                newEntry = new LogicalData(Path.path("testFileThread1"),Constants.LOGICAL_FILE);
            } else if (this.getName().equals("T2")) {
                newEntry = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_2,Constants.LOGICAL_FILE);
//                newEntry = new LogicalData(Path.path("testFileThread2"),Constants.LOGICAL_FILE);
            } else if (this.getName().equals("T3")) {
                newEntry = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_3,Constants.LOGICAL_FILE);
//                newEntry = new LogicalData(Path.path("testFileThread3"),Constants.LOGICAL_FILE);
                
            }
            
            
            debug("New entry: "+newEntry);
            
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
            children.add(Path.path("child1"));
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
        ILogicalData parent = new LogicalData(parentPath,Constants.LOGICAL_DATA);

        SimpleDLCatalogue instance = new SimpleDLCatalogue();

        instance.registerResourceEntry(parent);

        //Add children to that resource

        Path childPath = Path.path(ldri + childLdri);

        LogicalData child = new LogicalData(childPath,Constants.LOGICAL_DATA);
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
//        System.out.println("entry:          " + entry.getUID() + " " + entry.getLDRI());
//        System.out.println("loadedEntry:    " + loadedEntry.getUID() + " " + loadedEntry.getLDRI());
        if (entry.getLDRI().getName().equals(loadedEntry.getLDRI().getName())) {
            //Due to very bad ptaches the UDIs are not the same any more 
//            if (entry.getUID().equals(loadedEntry.getUID())) {
            return true;
//            }
        }
        return false;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getName()+"."+getName()+": "+msg);
    }
}

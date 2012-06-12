/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.test.MyAuth;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import nl.uva.cs.lobcder.util.DummyHttpServletRequest;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataResourceFactoryTest {

    private static String vphUserName;
    private static String vphPassword;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator + "test.proprties";
        Properties prop = TestSettings.getTestProperties(propBasePath);
        vphUserName = prop.getProperty("vph.username1");
        vphPassword = prop.getProperty("vph.password1");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        //clean up 
        String host = "localhost:8080";
        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result1 = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        if (result1 != null) {
            result1.delete();
        }
        WebDataDirResource result2 = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_2);
        if (result2 != null) {
            result2.delete();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testGetResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";
        String strPath = "/";
        WebDataResourceFactory instance = new WebDataResourceFactory();

        WebDataDirResource result = getTestDir(instance, host);

        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
        checkChildren(result, file);

        result.delete();
        result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        assertNull(result);

    }

    /**
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testCreateGetAndDeleteResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = getTestDir(instance, host);

        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        checkChildren(result, file);
        Long len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        String acceps = "text/html,text/*;q=0.9";
        String res = file.getContentType(acceps);
        String expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        Date date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        String name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        result.delete();
        result = (WebDataDirResource) (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        assertNull(result);

        instance = new WebDataResourceFactory();

        result = getTestDir(instance, host);

        bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
        checkChildren(result, file);
        len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        acceps = "text/html,text/*;q=0.9";
        res = file.getContentType(acceps);
        expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        name = file.getName();
        assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);

        result.delete();
        result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        assertNull(result);

    }

    @Test
    public void testCreateGetAndDeleteResource2() {

        System.out.println("getResource");
        String host = "localhost:8080";
        WebDataDirResource result = null;
        RDMSDLCatalog cat = null;
        ILogicalData entry = null;
        try {
            WebDataResourceFactory instance = new WebDataResourceFactory();

            result = getTestDir(instance, host);

            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
            checkChildren(result, file);
            Long len = file.getContentLength();
            assertEquals(len, new Long("DATA".getBytes().length));

            String acceps = "text/html,text/*;q=0.9";
            String res = file.getContentType(acceps);
            String expResult = "text/*;q=0.9";
            assertEquals(expResult, res);

            Date date = file.getCreateDate();
            assertNotNull(date);
            date = file.getModifiedDate();
            assertNotNull(date);

            String name = file.getName();
            assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
            List<? extends Resource> children = result.getChildren();
            assertFalse(children.isEmpty());
            boolean foundIt = false;
            for (Resource r : children) {
                if (r.getUniqueId().equals(file.getUniqueId())) {
                    foundIt = true;
                }
            }
            assertTrue(foundIt);
            cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            Collection<StorageSite> all = cat.getAllSites();

            file.delete();
            children = result.getChildren();
            //File should not be there 
            foundIt = false;
            for (Resource r : children) {
                if (r.getUniqueId().equals(file.getUniqueId())) {
                    foundIt = true;
                }
            }
            assertFalse(foundIt);

//        RDMSDLCatalog cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
            entry = cat.getResourceEntryByLDRI(file.getPath());
            assertNull(entry);

            //At this point the collection should exist
            result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
            checkResource(result);
            Collection<IStorageSite> sites = file.getStorageSites();
            assertFalse(sites.isEmpty());

//        System.out.println(">>>>>>Sites: " + sites.size());
//        for (IStorageSite s : sites) {
//            System.out.println(">>>>>>Sites: " + s.getEndpoint() + " " + s.getUID());
//        }

            file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long("DATA".getBytes().length), "text/plain");
            checkChildren(result, file);
            len = file.getContentLength();
            assertEquals(len, new Long("DATA".getBytes().length));


            acceps = "text/html,text/*;q=0.9";
            res = file.getContentType(acceps);
            expResult = "text/*;q=0.9";
            assertEquals(expResult, res);

            date = file.getCreateDate();
            assertNotNull(date);
            date = file.getModifiedDate();
            assertNotNull(date);


            name = file.getName();
            assertEquals(ConstantsAndSettings.TEST_FILE_NAME_1, name);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        } finally {
            try {
                result.delete();
                //        result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
                //        assertNull(result);
                cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
                entry = cat.getResourceEntryByLDRI(result.getPath());
                assertNull(entry);
            } catch (Exception ex) {
                Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

//    @Test
//    public void testStorageSites() throws Exception {
//
//        System.out.println("testStorageSites");
//        String host = "localhost:8080";
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        WebDataDirResource result = getTestDir(instance, host);
//
//        instance = new WebDataResourceFactory();
//        result = getTestDir(instance, host);
//
//        result.delete();
//        result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
//        assertNull(result);
//    }

    @Test
    public void testCreateAndGetResourceContent() throws Exception {
        System.out.println("testCreateAndGetResourceContent");
        String host = "localhost:8080";

        //1st PUT
        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = getTestDir(instance, host);

        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
        checkChildren(result, file);

        Long len = file.getContentLength();
        assertEquals(len, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length));

        //1st GET
        instance = new WebDataResourceFactory();
        WebDataFileResource reloadedFile = (WebDataFileResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1 + "/" + ConstantsAndSettings.TEST_FILE_NAME_1);

        checkResource(reloadedFile);
        compareResources(reloadedFile, file);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Range range = null;
        Map<String, String> params = null;
        String contentType = "text/plain";
        reloadedFile.sendContent(out, range, params, contentType);
        String content = new String(out.toByteArray());
        assertEquals(ConstantsAndSettings.TEST_DATA, content);

        result.delete();
        result = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        assertNull(result);
    }

//    @Test
//    public void testUpDownloadLargeFiles() throws NotAuthorizedException, ConflictException, BadRequestException {
//        System.out.println("testUpDownloadLargeFiles");
//        String host = "localhost:8080";
//        WebDataFileResource file = null;
//        WebDataDirResource dir = null;
//        try {
//            WebDataResourceFactory instance = new WebDataResourceFactory();
//            dir = getTestDir(instance, host);
//
//            int count = 1;
//            for (int i = 0; i < count; i++) {
//                File tmpLocalFile = File.createTempFile(this.getClass().getName(), null);
//                byte[] data = new byte[1024 * 200];//1MB
//                Random r = new Random();
//                r.nextBytes(data);
//
//                FileOutputStream fos = new FileOutputStream(tmpLocalFile);
//                fos.write(data);
//                fos.flush();
//                fos.close();
//
//                FileInputStream fins = new FileInputStream(tmpLocalFile);
//                file = (WebDataFileResource) dir.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, fins, new Long(tmpLocalFile.length()), "application/octet-stream");
//                checkChildren(dir, file);
//
//                Long len = file.getContentLength();
//                assertEquals(len, new Long(tmpLocalFile.length()));
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            dir.delete();
//        }
//    }
//
//    @Test
//    public void testMakeCollectionAddChildAndRenameChild() {
//        try {
//            System.out.println("testMakeCollectionAddChildAndRenameChild");
//            String host = "localhost:8080";
//            WebDataFileResource file = null;
//            WebDataDirResource dir = null;
//            RDMSDLCatalog cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
//
//            WebDataResourceFactory instance = new WebDataResourceFactory();
//            dir = getTestDir(instance, host);
//
//            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//            file = (WebDataFileResource) dir.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//            checkChildren(dir, file);
//            //Check if catalogue has the child entry
//            ILogicalData childEntry = cat.getResourceEntryByLDRI(file.getPath());
//            assertEquals(childEntry.getLDRI().toString(), file.getPath().toString());
//            assertEquals(childEntry.getUID(), file.getUniqueId());
//
//            //Check if the collection has the child
//            ILogicalData collectionEntry = cat.getResourceEntryByLDRI(dir.getPath());
//            Path childPath = collectionEntry.getChild(file.getPath());
//            assertNotNull(childPath);
//            assertEquals(childPath.toString(), file.getPath().toString());
//            collectionEntry = cat.getResourceEntryByLDRI(file.getPath().getParent());
//            childPath = collectionEntry.getChild(file.getPath());
//            assertNotNull(childPath);
//            assertEquals(childPath.toString(), file.getPath().toString());
//
//            file.moveTo(dir, ConstantsAndSettings.TEST_FILE_NAME_2);
//            checkChildren(dir, file);
//            //Get data 
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Range range = null;
//            Map<String, String> params = null;
//            String contentType = "text/plain";
//            file.sendContent(out, range, params, contentType);
//            String content = new String(out.toByteArray());
//            assertEquals(ConstantsAndSettings.TEST_DATA, content);
//
//            //Check if catalogue has the child entry with the new name 
//            //if we don't have new catalog we get back the old child entry
//            cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
//            childEntry = cat.getResourceEntryByLDRI(file.getPath());
//
//            assertEquals(childEntry.getLDRI().toString(), file.getPath().toString());
//            assertEquals(childEntry.getUID(), file.getUniqueId());
//
//            dir.delete();
//
//            //The child and the file should be gone 
//            ILogicalData lDir = cat.getResourceEntryByLDRI(Path.path(ConstantsAndSettings.TEST_FOLDER_NAME_1));
//            assertNull(lDir);
//            ILogicalData lf1 = cat.getResourceEntryByLDRI(Path.path(ConstantsAndSettings.TEST_FILE_NAME_1));
//            assertNull(lf1);
//            ILogicalData lf2 = cat.getResourceEntryByLDRI(Path.path(ConstantsAndSettings.TEST_FILE_NAME_2));
//            assertNull(lf2);
//
//
//        } catch (Exception ex) {
//            fail(ex.getMessage());
//            ex.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testGetTopLevel() throws Exception {
//        System.out.println("testMakeCollectionAddChildAndRenameChild");
//        String host = "localhost:8080";
//        WebDataFileResource file = null;
//        WebDataDirResource dir = null;
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
////        instance.setUserName(vphUserName);
//        WebDataDirResource root = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH);
////        root.authorise(null, Request.Method.HEAD, new Auth(vphUserName, new Object()));
//        assertNotNull(root);
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        file = (WebDataFileResource) root.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(root, file);
//
//        file.delete();
//    }
//
//    @Test
//    public void testInconsistency() {
//        try {
//            String host = "localhost:8080";
//            WebDataFileResource file = null;
//            WebDataDirResource dir = null;
//            CollectionResource dir2 = null;
//
//            WebDataResourceFactory instance;
//            try {
//                instance = new WebDataResourceFactory();
//                dir = getTestDir(instance, host);
//                dir2 = dir.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_2);
//                ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//                file = (WebDataFileResource) dir.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//                VFSNode node = file.getLogicalData().getVFSNode();
//                node.delete();
//            } catch (Exception ex) {
//                Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//
//
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Range range = null;
//            Map<String, String> params = null;
//            String contentType = "text/plain";
//            try {
//                file.sendContent(out, range, params, contentType);
//                fail("No exception");
//            } catch (Exception ex) {
//            }
//            dir.delete();
//        } catch (NotAuthorizedException ex) {
//            Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ConflictException ex) {
//            Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (BadRequestException ex) {
//            Logger.getLogger(WebDataResourceFactoryTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    @Test
//    public void testMake2Subcollections() throws Exception {
//        System.out.println("testMakeCollectionAddChildAndRenameChild");
//        String host = "localhost:8080";
//        WebDataFileResource file = null;
//        WebDataDirResource dir = null;
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        dir = getTestDir(instance, host);
//        dir.createCollection("subCollection1");
//
//        WebDataDirResource sub1 = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + "/" + dir.getName() + "/subCollection1");
//        assertNotNull(sub1);
//        dir.createCollection("subCollection2");
//        WebDataDirResource sub2 = (WebDataDirResource) getResource(instance, host, ConstantsAndSettings.CONTEXT_PATH + "/" + dir.getName() + "/subCollection2");
//
//        assertNotNull(sub2);
//
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        file = (WebDataFileResource) sub1.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(sub1, file);
//
//
//        dir.delete();
//    }
//
//    @Test
//    public void testDeleteFileWithSameStartingName() throws Exception {
//        System.out.println("testDeleteFileWithSameStartingName");
//        String host = "localhost:8080";
//        WebDataDirResource dir = null;
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        dir = getTestDir(instance, host);
//
//
//        WebDataDirResource coll1 = (WebDataDirResource) dir.createCollection("testFile1");
//        WebDataDirResource sub1 = (WebDataDirResource) coll1.createCollection("subCollection");
//        WebDataDirResource sub2 = (WebDataDirResource) sub1.createCollection("subSubCollection2");
//        WebDataDirResource sub3 = (WebDataDirResource) sub1.createCollection("subSubCollection3");
//        WebDataDirResource sub4 = (WebDataDirResource) sub3.createCollection("subSubSubCollection4");
//
//
//        ByteArrayInputStream bais10 = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file10 = (WebDataFileResource) dir.createNew("testFile10", bais10, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(dir, file10);
//
//        ByteArrayInputStream bais100 = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file100 = (WebDataFileResource) dir.createNew("testFile100", bais100, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//        checkChildren(dir, file100);
//
//
//        coll1.delete();
//
//        //file10 and file100 should still be there
//        assertNotNull(dir.child(file10.getName()));
//        checkIfResourceExists(file10, ConstantsAndSettings.TEST_DATA);
//
//        assertNotNull(dir.child(file100.getName()));
//        checkIfResourceExists(file100, ConstantsAndSettings.TEST_DATA);
//
//
//        //sub1, sub2, sub3, sub4 should not be there
//        RDMSDLCatalog cat = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));
//        assertNull(cat.getResourceEntryByLDRI(sub1.getPath()));
//        assertNull(cat.getResourceEntryByLDRI(sub2.getPath()));
//        assertNull(cat.getResourceEntryByLDRI(sub3.getPath()));
//        assertNull(cat.getResourceEntryByLDRI(sub4.getPath()));
//
//        dir.delete();
//    }
//
//    @Test
//    public void testDeleteAndCheckLogicalPaths() throws Exception {
//        String host = "localhost:8080";
//        WebDataDirResource dir = null;
//
//        WebDataResourceFactory instance = new WebDataResourceFactory();
//        dir = getTestDir(instance, host);
//
//        ByteArrayInputStream bais1 = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//        WebDataFileResource file1 = (WebDataFileResource) dir.createNew("testFile1", bais1, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//    }
//
//    @Test
//    public void testMultiThread() throws FileNotFoundException, IOException {
//        try {
//            System.out.println("testMultiThread");
//            Thread userThread1 = new UserThread(1);
//            userThread1.setName("T1");
////
//            Thread userThread2 = new UserThread(1);
//            userThread2.setName("T2");
////
////            Thread userThread3 = new UserThread(2);
////            userThread3.setName("T3");
//
//
//            userThread1.start();
//            userThread2.start();
////            userThread3.start();
//
//            userThread1.join();
//            userThread2.join();
////            userThread3.join();
//        } catch (InterruptedException ex) {
//            fail();
//            ex.printStackTrace();
//        }
//    }

    private void checkChildren(WebDataDirResource result, WebDataFileResource file) throws NotAuthorizedException {
        List<? extends Resource> children = result.getChildren();
        assertFalse(children.isEmpty());
        boolean foundIt = false;
        for (Resource r : children) {
            if (r.getUniqueId().equals(file.getUniqueId()) && r.getName().equals(file.getName())) {
                foundIt = true;
            }
        }
        assertTrue(foundIt);
    }

    private void checkResource(WebDataResource result) throws VlException, CatalogueException, IOException {
        assertNotNull(result);
        Collection<IStorageSite> sites = result.getStorageSites();
        assertFalse(sites.isEmpty());
        for (IStorageSite s : sites) {
            assertNotNull(s.getCredentials());
            assertNotNull(s.getEndpoint());
            assertNotNull(s.getVPHUsernames());
            assertNotNull(s.getCredentials().getStorageSiteUsername());
            VFSNode node = s.createVFSFile(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertNotNull(node);
            assertTrue(node.exists());
            node = s.getVNode(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertNotNull(node);
            assertTrue(node.exists());

            s.deleteVNode(ConstantsAndSettings.TEST_FILE_PATH_1);

            assertFalse(node.exists());
            boolean hasData = s.LDRIHasPhysicalData(ConstantsAndSettings.TEST_FILE_PATH_1);
            assertFalse(hasData);
        }
        Metadata meta = result.getLogicalData().getMetadata();
        assertNotNull(meta);
//        assertNotNull(meta.getPermissionArray());
    }

    private WebDataDirResource getTestDir(WebDataResourceFactory instance, String host) throws NotAuthorizedException, ConflictException, BadRequestException, VlException, CatalogueException, IOException, MyPrincipal.Exception {
        //        instance.setUserName(vphUserName);
//        DummyHttpServletRequest req = new DummyHttpServletRequest();
//        req.getSession().setAttribute("vph-user", new MyPrincipal(vphUserName+vphPassword, MyAuth.getInstance().checkToken(vphUserName+vphPassword)));
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH + ConstantsAndSettings.TEST_FOLDER_NAME_1);
        if (result == null) {
            WebDataDirResource root = (WebDataDirResource) instance.getResource(host, ConstantsAndSettings.CONTEXT_PATH);
//            root.authorise(null, Request.Method.HEAD, new Auth(vphUserName, new Object()));
            HttpServletRequest r = new DummyHttpServletRequest();
            WebDavServlet.setThreadlocals(r, null);
            root.authenticate(vphUserName, vphPassword);
            assertNotNull(root);
            result = (WebDataDirResource) root.createCollection(ConstantsAndSettings.TEST_FOLDER_NAME_1);
        }
//        result.authorise(null, Request.Method.HEAD, new Auth(vphUserName, new Object()));
        result.authenticate(vphUserName, vphPassword);
        checkResource(result);
        return result;
    }

    private void checkIfResourceExists(WebDataFileResource file10, String TEST_DATA) throws VlException, CatalogueException, IOException {
        Collection<IStorageSite> sites = file10.getStorageSites();
        boolean foundIt = false;
        VFSNode node = null;
        for (IStorageSite s : sites) {
            try {
                node = s.getVNode(file10.getPath());
            } catch (ResourceNotFoundException ex) {
                //That's ok
                node = null;
            }
            Collection<String> lp = s.getLogicalPaths();
            if (node != null && node.exists()) {
                assertTrue(lp.contains(file10.getPath().toString()));
                assertTrue(s.LDRIHasPhysicalData(file10.getPath()));
                assertEquals(((VFile) node).getContentsAsString(), TEST_DATA);
                foundIt = true;
            } else {
                assertFalse(lp.contains(file10.getPath().toString()));
                assertFalse(s.LDRIHasPhysicalData(file10.getPath()));
            }

        }
        assertTrue(foundIt);

    }

    private void compareResources(WebDataFileResource reloadedFile, WebDataFileResource file) {
        assertNotNull(reloadedFile);
        assertNotNull(file);
        assertEquals(reloadedFile.getUniqueId(), file.getUniqueId());
        assertEquals(reloadedFile.getPath().toString(), file.getPath().toString());
        assertEquals(reloadedFile.getLogicalData().getParent(), file.getLogicalData().getParent());
        assertNotNull(reloadedFile.getLogicalData().getMetadata());
        assertNotNull(file.getLogicalData().getMetadata());
        assertEquals(reloadedFile.getLogicalData().getMetadata().getContentTypes(), file.getLogicalData().getMetadata().getContentTypes());
        assertEquals(reloadedFile.getLogicalData().getMetadata().getCreateDate(), file.getLogicalData().getMetadata().getCreateDate());
        assertEquals(reloadedFile.getLogicalData().getMetadata().getLength(), file.getLogicalData().getMetadata().getLength());
        assertEquals(reloadedFile.getLogicalData().getMetadata().getModifiedDate(), file.getLogicalData().getMetadata().getModifiedDate());
        assertEquals(reloadedFile.getLogicalData().getMetadata().getPermissionArray(), file.getLogicalData().getMetadata().getPermissionArray());
    }

    private WebDataResource getResource(WebDataResourceFactory instance, String host, String path) {
        WebDataResource resource = (WebDataResource) instance.getResource(host, path);
        if (resource != null) {
            HttpServletRequest r = new DummyHttpServletRequest();
            WebDavServlet.setThreadlocals(r, null);
            resource.authenticate(vphUserName, vphPassword);
        }
        return resource;
    }
}

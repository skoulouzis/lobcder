/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import nl.uva.cs.lobcder.util.ContantsAndSettings;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.resources.ILogicalData;
import com.bradmcevoy.common.Path;
import java.io.ByteArrayInputStream;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import java.io.ByteArrayOutputStream;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.resources.StorageSiteManager;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
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
public class WebDataFileResourceTest {

    private SimpleDLCatalogue catalogue;
    private LogicalFile testLogicalFile;
    private Path testFolderPath;
    private ArrayList<StorageSite> sites;
    private LogicalFolder testLogicalFolder;
    private StorageSite site;

    public WebDataFileResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try {
            catalogue = new SimpleDLCatalogue();

            testLogicalFile = new LogicalFile(ContantsAndSettings.TEST_FILE_PATH);

            //For some reason 'testFolder' creates an exception with data nucleus
            testFolderPath = Path.path(ContantsAndSettings.TEST_FOLDER_NAME);
            testLogicalFolder = new LogicalFolder(testFolderPath);

            String endpoint = "file:///tmp/";
            String vphUser = "user1";
            Credential cred = new Credential(vphUser);
            site = new StorageSite(endpoint, cred);

            sites = new ArrayList<StorageSite>();
            sites.add(site);

        } catch (Exception ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
    }

    @After
    public void tearDown() {
        sites.clear();
        site = null;
        sites = null;
    }

    /**
     * Test of copyTo method, of class WebDataFileResource.
     */
    @Test
    public void testCopyTo() {
        System.out.println("copyTo");
        WebDataDirResource collectionResource = null;
        WebDataFileResource webDAVFile = null;
        ILogicalData chLData = null;
        ILogicalData load;
        try {
            String testColl = "testCopyToColl";
            Path testCollPath = Path.path(testColl);
            testLogicalFolder = new LogicalFolder(testCollPath);

            catalogue.registerResourceEntry(testLogicalFolder);
            collectionResource = new WebDataDirResource(catalogue, testLogicalFolder);

            webDAVFile = new WebDataFileResource(catalogue, testLogicalFile);
            webDAVFile.copyTo(collectionResource, webDAVFile.getName());


            ILogicalData folderLData = catalogue.getResourceEntryByLDRI(testCollPath);
            Collection<Path> children = folderLData.getChildren();
            assertNotNull(children);

            boolean foundIt = false;
            for (Path p : children) {
                chLData = catalogue.getResourceEntryByLDRI(p);
                System.out.println("LData:              " + chLData.getLDRI().getName() + "         " + chLData.getUID());
                System.out.println("webDAVFile:         " + webDAVFile.getName() + "            " + webDAVFile.getUniqueId());
                System.out.println("testLogicalFile:    " + testLogicalFile.getLDRI().getName() + "         " + testLogicalFile.getUID());
                if (chLData.getLDRI().getName().equals(ContantsAndSettings.TEST_FILE_NAME)) {
                    foundIt = true;
                    break;
                }
            }
            assertTrue(foundIt);


            collectionResource = new WebDataDirResource(catalogue, folderLData);
            List<? extends Resource> webChildren = collectionResource.getChildren();
            for (Resource r : webChildren) {
                System.out.println("Children: " + r.getName() + " " + r.getUniqueId());
                if (r.getName().equals(ContantsAndSettings.TEST_FILE_NAME)) {
                    foundIt = true;
                    break;
                }
            }

            assertTrue(foundIt);

        } catch (Exception ex) {
            fail(ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {


                load = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
                assertNotNull(load);

                collectionResource.delete();

                load = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
                assertNull(load);

                load = catalogue.getResourceEntryByLDRI(Path.path(testLogicalFolder.getLDRI(), ContantsAndSettings.TEST_FILE_NAME));
                assertNull(load);

            } catch (CatalogueException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotAuthorizedException ex) {
                fail(ex.getMessage());
            } catch (ConflictException ex) {
                fail(ex.getMessage());
            } catch (BadRequestException ex) {
                fail(ex.getMessage());
            }
        }
    }

//    /**
//     * Test of delete method, of class WebDataFileResource.
//     */
//    @Test
//    public void testDelete() {
//        System.out.println("delete");
//
//        try {
//            catalogue.registerResourceEntry(testLogicalFile);
//
//            WebDataFileResource instance = new WebDataFileResource(catalogue, testLogicalFile);
//            instance.delete();
//
//            ILogicalData result = catalogue.getResourceEntryByLDRI(ContantsAndSettings.TEST_FILE_PATH);
//            assertNull(result);
//
//        } catch (NotAuthorizedException ex) {
//            fail(ex.getMessage());
//        } catch (ConflictException ex) {
//            fail(ex.getMessage());
//        } catch (BadRequestException ex) {
//            fail(ex.getMessage());
//        } catch (CatalogueException ex) {
//            fail(ex.getMessage());
//        } finally {
//        }
//    }
//
//    /**
//     * Test of getContentLength method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetContentLength() {
//        System.out.println("getContentLength");
//        WebDataDirResource coll = null;
//        WebDataFileResource instance = null;
//        ILogicalData load;
//        try {
//            ByteArrayInputStream bais = new ByteArrayInputStream(ContantsAndSettings.TEST_DATA.getBytes());
//
//            testLogicalFolder.setStorageSites(sites);
//            catalogue.registerResourceEntry(testLogicalFolder);
//
//            ILogicalData loaded = catalogue.getResourceEntryByLDRI(testFolderPath);
//            coll = new WebDataDirResource(catalogue, loaded);
//
//            instance = (WebDataFileResource) coll.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long(ContantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//
//            Long result = instance.getContentLength();
//
//            Long exp = new Long(ContantsAndSettings.TEST_DATA.getBytes().length);
//            assertEquals(exp, result);
//
//        } catch (Exception ex) {
//            fail(ex.getMessage());
//        } finally {
//            try {
//                instance.delete();
//                coll.delete();
//
//                load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
//                assertNull(load);
//
//                load = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//                assertNull(load);
//
//                new StorageSiteManager().clearAllSites();
//
//            } catch (CatalogueException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
//    /**
//     * Test of getContentType method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetContentType() {
//        System.out.println("getContentType");
//        WebDataFileResource instance = null;
//        String acceps = "text/html,text/*;q=0.9,image/jpeg;q=0.9,image/png;q=0.9,image/*;q=0.9,*/*;q=0.8";
//        ILogicalData load;
//        try {
//            Metadata meta = testLogicalFile.getMetadata();
//            meta.addContentType("text/plain");
//            testLogicalFile.setMetadata(meta);
//
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//            String expResult = "text/*;q=0.9";
//            String result = instance.getContentType(acceps);
//
//            assertEquals(expResult, result);
//
//
//            //@TODO Test more content types             
////            meta = testLogicalFile.getMetadata();
////            meta.addContentType("text/html");
////            testLogicalFile.setMetadata(meta);
////            
////            instance = new WebDataFileResource(catalogue, testLogicalFile);
////            expResult = "text/*;q=0.9";
////            result = instance.getContentType(acceps);            
////            assertEquals(expResult, result);
//        } catch (CatalogueException ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                instance.delete();
////                catalogue.unregisterResourceEntry(testLogicalFile);
//                load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
//                assertNull(load);
//
//                new StorageSiteManager().clearAllSites();
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (CatalogueException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
////    /**
////     * Test of getMaxAgeSeconds method, of class WebDataFileResource.
////     */
////    @Test
////    public void testGetMaxAgeSeconds() {
////        System.out.println("getMaxAgeSeconds");
////        Auth auth = null;
////        WebDataFileResource instance = null;
////        Long expResult = null;
////        Long result = instance.getMaxAgeSeconds(auth);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
//    /**
//     * Test of sendContent method, of class WebDataFileResource.
//     */
//    @Test
//    public void testSendContent() throws Exception {
//        System.out.println("sendContent");
//        ByteArrayOutputStream out = null;
//        Range range = null;
//        Map<String, String> params = null;
//        String contentType = "text/plain";
//        WebDataFileResource instance = null;
//        try {
//            VFSNode node = site.createVFSFile(ContantsAndSettings.TEST_FILE_PATH);
//            ((VFile) node).setContents(ContantsAndSettings.TEST_DATA);
//
//            sites.add(site);
//            testLogicalFile.setStorageSites(sites);
//            catalogue.registerResourceEntry(testLogicalFile);
//            //If we don't reload the logical file, metadata and storage sites are set to null
//            ILogicalData loadedLFile = catalogue.getResourceEntryByLDRI(ContantsAndSettings.TEST_FILE_PATH);
//
//            out = new ByteArrayOutputStream();
//
//            instance = new WebDataFileResource(catalogue, loadedLFile);
//            instance.sendContent(out, range, params, contentType);
//            String result = new String(out.toByteArray());
//            assertEquals(ContantsAndSettings.TEST_DATA, result);
//
//
//            out.reset();
//            range = new Range(0, 50);
//            instance.sendContent(out, range, params, contentType);
//            result = new String(out.toByteArray());
//            assertEquals(ContantsAndSettings.TEST_DATA.subSequence(0, 50), result);
//
//        } finally {
//            instance.delete();
//            ILogicalData load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
//            assertNull(load);
//
//            new StorageSiteManager().clearAllSites();
//        }
//    }
//
//    /**
//     * Test of moveTo method, of class WebDataFileResource.
//     */
//    @Test
//    public void testMoveTo() throws Exception {
//        System.out.println("moveTo");
//        WebDataDirResource rDest = null;
//        WebDataFileResource instance = null;
//        Path path;
//
//        try {
//            catalogue.registerResourceEntry(testLogicalFolder);
//            catalogue.registerResourceEntry(testLogicalFile);
//
//            rDest = new WebDataDirResource(catalogue, testLogicalFolder);
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//            instance.moveTo(rDest, ContantsAndSettings.TEST_FILE_NAME);
//
//
//            ILogicalData loadedLFolder = catalogue.getResourceEntryByLDRI(testFolderPath);
//            assertNotNull(loadedLFolder);
//            ILogicalData loadedLFile = catalogue.getResourceEntryByLDRI(Path.path(testFolderPath, ContantsAndSettings.TEST_FILE_NAME));
//            assertNotNull(loadedLFile);
//
//            rDest = new WebDataDirResource(catalogue, loadedLFolder);
//            assertNotNull(rDest);
//            instance = new WebDataFileResource(catalogue, loadedLFile);
//            assertNotNull(rDest);
//            List<? extends Resource> children = rDest.getChildren();
//
//            assertNotNull(children);
//            assertFalse(children.isEmpty());
//            boolean foundIt = false;
//            for (Resource r : children) {
//                path = ((WebDataFileResource) r).getPath();
//                if (path.equals(instance.getPath())) {
//                    foundIt = true;
//                }
//            }
//            assertTrue(foundIt);
//        } finally {
//            instance.delete();
//            rDest.delete();
//        }
//    }
//
////    /**
////     * Test of processForm method, of class WebDataFileResource.
////     */
////    @Test
////    public void testProcessForm() throws Exception {
////        System.out.println("processForm");
////        Map<String, String> params = new HashMap<String, String>();
////        Map<String, FileItem> files = new HashMap<String, FileItem>();
////        WebDataFileResource instance = null;
////        try {
////
////            boolean isFormField = false;
////            int sizeThreshold = -1;
////            File repository = new File(System.getProperty("java.io.tmpdir"));
////
////            org.apache.commons.fileupload.disk.DiskFileItem fItem = new org.apache.commons.fileupload.disk.DiskFileItem("file", "text/plain", isFormField, testFileName, sizeThreshold, repository);
////            OutputStream out = fItem.getOutputStream();
////            out.write(testData.getBytes());
////            out.flush();
////            out.close();
////
////            assertNotNull(fItem);
////            assertEquals(fItem.getSize(), new Long(testData.getBytes().length).longValue());
////            assertEquals(fItem.getContentType(), "text/plain");
////            assertEquals(fItem.getFieldName(), "file");
////            assertEquals(fItem.getString(), testData);
////
////            FileItemWrapper fIW = new FileItemWrapper(fItem);
////
////            files.put("file1", fIW);
////
////
////            fItem = new org.apache.commons.fileupload.disk.DiskFileItem("file", "text/plain", isFormField, testFileName, sizeThreshold, repository);
////            out = fItem.getOutputStream();
////            out.write(testData.getBytes());
////            out.flush();
////            out.close();
////            
////            assertNotNull(fItem);
////            assertEquals(fItem.getSize(), new Long(testData.getBytes().length).longValue());
////            assertEquals(fItem.getContentType(), "text/plain");
////            assertEquals(fItem.getFieldName(), "file");
////            assertEquals(fItem.getString(), testData);
////
////            fIW = new FileItemWrapper(fItem);
////            
////            files.put("file2", fIW);
////
////            instance = new WebDataFileResource(catalogue, testLogicalFile);
////            String processForm = instance.processForm(params, files);
////
////        } finally {
////            instance.delete();
////            ILogicalData load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
////            assertNull(load);
////        }
////    }
//    /**
//     * Test of getUniqueId method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetUniqueId() {
//        WebDataFileResource instance = null;
//        try {
//            System.out.println("getUniqueId");
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//            String expResult = testLogicalFile.getUID();
//            String result = instance.getUniqueId();
//            assertEquals(expResult, result);
//            assertEquals(testLogicalFile.getUID(), result);
//        } catch (CatalogueException ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                instance.delete();
//                ILogicalData load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
//                assertNull(load);
//            } catch (CatalogueException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//
//    /**
//     * Test of getName method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetName() {
//        WebDataFileResource instance = null;
//        try {
//            System.out.println("getName");
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//            String expResult = testLogicalFile.getLDRI().getName();
//            String result = instance.getName();
//            assertEquals(expResult, result);
//        } catch (CatalogueException ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                instance.delete();
//                ILogicalData load = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
//                assertNull(load);
//            } catch (CatalogueException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
////    /**
////     * Test of authenticate method, of class WebDataFileResource.
////     */
////    @Test
////    public void testAuthenticate() {
////        System.out.println("authenticate");
////        String user = "";
////        String password = "";
////        WebDataFileResource instance = null;
////        Object expResult = null;
////        Object result = instance.authenticate(user, password);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of authorise method, of class WebDataFileResource.
////     */
////    @Test
////    public void testAuthorise() {
////        System.out.println("authorise");
////        Request request = null;
////        Method method = null;
////        Auth auth = null;
////        WebDataFileResource instance = null;
////        boolean expResult = false;
////        boolean result = instance.authorise(request, method, auth);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
////    /**
////     * Test of getRealm method, of class WebDataFileResource.
////     */
////    @Test
////    public void testGetRealm() {
////        System.out.println("getRealm");
////        WebDataFileResource instance = null;
////        String expResult = "";
////        String result = instance.getRealm();
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
////
//
//    /**
//     * Test of getModifiedDate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetModifiedDate() {
//        WebDataFileResource instance = null;
//        try {
//            System.out.println("getModifiedDate");
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//
//            Date result = instance.getModifiedDate();
//            assertNotNull(result);
//
//        } catch (CatalogueException ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                instance.delete();
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//    
//    
//    
//    
//    /**
//     * Test of getModifiedDate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetCreateDate() {
//        WebDataFileResource instance = null;
//        try {
//            System.out.println("getModifiedDate");
//            instance = new WebDataFileResource(catalogue, testLogicalFile);
//
//            Date result = instance.getCreateDate();
//            assertNotNull(result);
//
//        } catch (CatalogueException ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                instance.delete();
//            } catch (NotAuthorizedException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ConflictException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (BadRequestException ex) {
//                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
////
////    /**
////     * Test of checkRedirect method, of class WebDataFileResource.
////     */
////    @Test
////    public void testCheckRedirect() {
////        System.out.println("checkRedirect");
////        Request request = null;
////        WebDataFileResource instance = null;
////        String expResult = "";
////        String result = instance.checkRedirect(request);
////        assertEquals(expResult, result);
////        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
////    }
}

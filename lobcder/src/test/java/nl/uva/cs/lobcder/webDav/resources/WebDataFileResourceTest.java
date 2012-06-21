/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.ConstantsAndSettings;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
import nl.uva.cs.lobcder.frontend.WebDavServlet;
import nl.uva.cs.lobcder.resources.*;
import nl.uva.cs.lobcder.util.DummyHttpServletRequest;
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

    private RDMSDLCatalog catalogue;
    private ArrayList<IStorageSite> sites;
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
            catalogue = new RDMSDLCatalog(new File(nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR + "/datanucleus.properties"));

            String endpoint = "file:///tmp/";
            String vphUser = "user1";
            Credential cred = new Credential(vphUser.split(","));
            site = new StorageSite(endpoint, cred);

            sites = new ArrayList<IStorageSite>();
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
    public void testCopyTo() throws Exception {
        System.out.println("copyTo");
        WebDataDirResource collectionResource = null;
        WebDataFileResource webDAVFile = null;
        ILogicalData chLData = null;
        ILogicalData load;
        LogicalData testLogicalFolder = null;
        String testFile = "testCopyTo";//ConstantsAndSettings.TEST_FILE_NAME_1;
        try {
            String testColl = "testCopyToColl";
            Path testCollPath = Path.path(testColl);
            testLogicalFolder = new LogicalData(testCollPath, Constants.LOGICAL_FOLDER);
            catalogue.registerResourceEntry(testLogicalFolder);
            collectionResource = createDirResource(catalogue, testLogicalFolder);


            Path testFilePath = Path.path(testFile);
            LogicalData testLogicalFile = new LogicalData(testFilePath, Constants.LOGICAL_FILE);
            webDAVFile = createFileResource(catalogue, testLogicalFile);
            webDAVFile.copyTo(collectionResource, webDAVFile.getName());
            assertNotNull(catalogue.getResourceEntryByLDRI(webDAVFile.getPath()));


            ILogicalData folderLData = catalogue.getResourceEntryByLDRI(testCollPath);
            assertEquals(testLogicalFolder.getType(), folderLData.getType());
            Collection<String> children = folderLData.getChildren();
            assertNotNull(children);

            boolean foundIt = false;
            for (String p : children) {
                chLData = catalogue.getResourceEntryByLDRI(Path.path(p));
                System.out.println("LData:              " + chLData.getLDRI().getName() + "         " + chLData.getUID());
                System.out.println("webDAVFile:         " + webDAVFile.getName() + "            " + webDAVFile.getUniqueId());
                System.out.println("testLogicalFolder:    " + testLogicalFolder.getLDRI().getName() + "         " + testLogicalFolder.getUID());
                if (chLData.getLDRI().getName().equals(testFile)) {
                    foundIt = true;
                    break;
                }
            }
            assertTrue(foundIt);

            collectionResource = createDirResource(catalogue, folderLData);
            List<? extends Resource> webChildren = collectionResource.getChildren();
            for (Resource r : webChildren) {
                System.out.println("Children: " + r.getName() + " " + r.getUniqueId());
                if (r.getName().equals(testFile)) {
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

                load = catalogue.getResourceEntryByLDRI(Path.path(testLogicalFolder.getLDRI(), testFile));
                assertNull(load);

                webDAVFile.delete();
                assertNull(catalogue.getResourceEntryByLDRI(webDAVFile.getPath()));


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

    /**
     * Test of delete method, of class WebDataFileResource.
     */
    @Test
    public void testDelete() throws Exception {
        System.out.println("delete");

        try {
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFile);

            WebDataFileResource instance = createFileResource(catalogue, testLogicalFile);
            instance.delete();

            ILogicalData result = catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI());
            assertNull(result);

        } catch (NotAuthorizedException ex) {
            fail(ex.getMessage());
        } catch (ConflictException ex) {
            fail(ex.getMessage());
        } catch (BadRequestException ex) {
            fail(ex.getMessage());
        } catch (CatalogueException ex) {
            fail(ex.getMessage());
        } finally {
        }
    }

    /**
     * Test of getContentLength method, of class WebDataFileResource.
     */
    @Test
    public void testGetContentLength() throws Exception {
        System.out.println("getContentLength");
        WebDataDirResource coll = null;
        WebDataFileResource instance = null;
        ILogicalData load;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
            LogicalData testLogicalFolder = new LogicalData(Path.path("testCollPath"), Constants.LOGICAL_FOLDER);

            testLogicalFolder.setStorageSites(sites);
            catalogue.registerResourceEntry(testLogicalFolder);

            ILogicalData loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
            coll = createDirResource(catalogue, loaded);

            instance = (WebDataFileResource) coll.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");

            Long result = instance.getContentLength();

            Long exp = new Long(ConstantsAndSettings.TEST_DATA.getBytes().length);
            assertEquals(exp, result);

        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            try {
                instance.delete();
                coll.delete();

                load = catalogue.getResourceEntryByLDRI(instance.getPath());
                assertNull(load);

                load = catalogue.getResourceEntryByLDRI(coll.getPath());
                assertNull(load);

            } catch (CatalogueException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Test of getContentType method, of class WebDataFileResource.
     */
    @Test
    public void testGetContentType() throws Exception {
        System.out.println("getContentType");
        WebDataFileResource instance = null;
        String acceps = "text/html,text/*;q=0.9,image/jpeg;q=0.9,image/png;q=0.9,image/*;q=0.9,*/*;q=0.8";
        ILogicalData load;
        LogicalData testLogicalFile = null;
        try {
            testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            Metadata meta = testLogicalFile.getMetadata();
            meta.addContentType("text/plain");
            testLogicalFile.setMetadata(meta);
            catalogue.registerResourceEntry(testLogicalFile);


            instance = createFileResource(catalogue, testLogicalFile);
            String expResult = "text/*;q=0.9";
            String result = instance.getContentType(acceps);

            assertEquals(expResult, result);


            //@TODO Test more content types             
//            meta = testLogicalFolder.getMetadata();
//            meta.addContentType("text/html");
//            testLogicalFolder.setMetadata(meta);
//            
//            instance = createFileResource(catalogue, testLogicalFolder);
//            expResult = "text/*;q=0.9";
//            result = instance.getContentType(acceps);            
//            assertEquals(expResult, result);
        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.delete();
                load = catalogue.getResourceEntryByLDRI(instance.getPath());
                assertNull(load);


            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (CatalogueException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

//    /**
//     * Test of getMaxAgeSeconds method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetMaxAgeSeconds() {
//        System.out.println("getMaxAgeSeconds");
//        Auth auth = null;
//        WebDataFileResource instance = null;
//        Long expResult = null;
//        Long result = instance.getMaxAgeSeconds(auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
    /**
     * Test of sendContent method, of class WebDataFileResource.
     */
    @Test
    public void testSendContent() throws Exception {
        System.out.println("sendContent");
        ByteArrayOutputStream out = null;
        Range range = null;
        Map<String, String> params = null;
        String contentType = "text/plain";
        WebDataFileResource instance = null;
        try {
            VFSNode node = site.createVFSFile(ConstantsAndSettings.TEST_FILE_PATH_1);
            ((VFile) node).setContents(ConstantsAndSettings.TEST_DATA);
            sites.add(site);

            LogicalData testLogicalFile = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_1, Constants.LOGICAL_FILE);
            testLogicalFile.setStorageSites(sites);
            catalogue.registerResourceEntry(testLogicalFile);
            //If we don't reload the logical file, metadata and storage sites are set to null
//            ILogicalData loadedLFile = catalogue.getResourceEntryByLDRI(ConstantsAndSettings.TEST_FILE_PATH_1);

            out = new ByteArrayOutputStream();
            instance = createFileResource(catalogue, testLogicalFile);
            instance.sendContent(out, range, params, contentType);
            String result = new String(out.toByteArray());
            assertEquals(ConstantsAndSettings.TEST_DATA, result);


            out.reset();
            range = new Range(0, 50);
            instance.sendContent(out, range, params, contentType);
            result = new String(out.toByteArray());
            assertEquals(ConstantsAndSettings.TEST_DATA.subSequence(0, 50), result);

        } finally {
            instance.delete();
            ILogicalData load = catalogue.getResourceEntryByLDRI(instance.getPath());
            assertNull(load);
        }
    }

    /**
     * Test of moveTo method, of class WebDataFileResource.
     */
    @Test
    public void testMoveTo() throws Exception {
        System.out.println("moveTo");
        WebDataDirResource rDest = null;
        WebDataFileResource instance = null;
        Path path;

        try {
            LogicalData testLogicalFolder = new LogicalData(Path.path("testCollPath"), Constants.LOGICAL_FOLDER);
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFolder);
            catalogue.registerResourceEntry(testLogicalFile);


            rDest = createDirResource(catalogue, testLogicalFolder);

            instance = createFileResource(catalogue, testLogicalFile);
            instance.moveTo(rDest, "newFileName");


            ILogicalData loadedLFolder = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
            assertNotNull(loadedLFolder);
            assertNotNull(loadedLFolder.getChild(Path.path(rDest.getPath(), "newFileName")));

            ILogicalData loadedLFile = catalogue.getResourceEntryByLDRI(Path.path(rDest.getPath(), "newFileName"));
            assertNotNull(loadedLFile);
            assertNull(catalogue.getResourceEntryByLDRI(testLogicalFile.getLDRI()));


            rDest = createDirResource(catalogue, loadedLFolder);
            assertNotNull(rDest);
            instance = createFileResource(catalogue, loadedLFile);
            assertNotNull(instance);
            List<? extends Resource> children = rDest.getChildren();

            assertNotNull(children);
            assertFalse(children.isEmpty());
            boolean foundIt = false;
            for (Resource r : children) {
                path = ((WebDataFileResource) r).getPath();
                if (path.equals(instance.getPath())) {
                    foundIt = true;
                }
            }
            assertTrue(foundIt);
        } finally {
            instance.delete();
            rDest.delete();
        }
    }

    /**
     * Test of processForm method, of class WebDataFileResource.
     */
    @Test
    public void testProcessForm() throws Exception {
        System.out.println("processForm");
        Map<String, String> params = new HashMap<String, String>();
        Map<String, FileItem> files = new HashMap<String, FileItem>();
        WebDataFileResource instance = null;
        try {

            boolean isFormField = false;
            int sizeThreshold = -1;
            File repository = new File(System.getProperty("java.io.tmpdir"));
            String testFileName = "testFileName";

            org.apache.commons.fileupload.disk.DiskFileItem fItem = new org.apache.commons.fileupload.disk.DiskFileItem("file", "text/plain", isFormField, testFileName, sizeThreshold, repository);
            OutputStream out = fItem.getOutputStream();
            out.write(ConstantsAndSettings.TEST_DATA.getBytes());
            out.flush();
            out.close();

            assertNotNull(fItem);
            assertEquals(fItem.getSize(), new Long(ConstantsAndSettings.TEST_DATA.getBytes().length).longValue());
            assertEquals(fItem.getContentType(), "text/plain");
            assertEquals(fItem.getFieldName(), "file");
            assertEquals(fItem.getString(), ConstantsAndSettings.TEST_DATA);

            FileItemWrapper fIW = new FileItemWrapper(fItem);

            files.put("file1", fIW);


            fItem = new org.apache.commons.fileupload.disk.DiskFileItem("file", "text/plain", isFormField, testFileName, sizeThreshold, repository);
            out = fItem.getOutputStream();
            out.write(ConstantsAndSettings.TEST_DATA.getBytes());
            out.flush();
            out.close();

            assertNotNull(fItem);
            assertEquals(fItem.getSize(), new Long(ConstantsAndSettings.TEST_DATA.getBytes().length).longValue());
            assertEquals(fItem.getContentType(), "text/plain");
            assertEquals(fItem.getFieldName(), "file");
            assertEquals(fItem.getString(), ConstantsAndSettings.TEST_DATA);

            fIW = new FileItemWrapper(fItem);

            files.put("file2", fIW);
            LogicalData testLogicalfile = new LogicalData(Path.path("testFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalfile);

            instance = createFileResource(catalogue, testLogicalfile);
            String processForm = instance.processForm(params, files);

        } finally {
            instance.delete();
            ILogicalData load = catalogue.getResourceEntryByLDRI(instance.getPath());
            assertNull(load);
        }
    }

    /**
     * Test of getUniqueId method, of class WebDataFileResource.
     */
    @Test
    public void testGetUniqueId() throws Exception {
        WebDataFileResource instance = null;
        try {
            System.out.println("getUniqueId");
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFile);

            instance = createFileResource(catalogue, testLogicalFile);
            String expResult = testLogicalFile.getUID();
            String result = instance.getUniqueId();
            assertEquals(expResult, result);
            assertEquals(testLogicalFile.getUID(), result);
        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.delete();
                ILogicalData load = catalogue.getResourceEntryByLDRI(instance.getPath());
                assertNull(load);
            } catch (CatalogueException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Test of getName method, of class WebDataFileResource.
     */
    @Test
    public void testGetName() throws Exception {
        WebDataFileResource instance = null;
        try {
            System.out.println("getName");
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFile);
            instance = createFileResource(catalogue, testLogicalFile);
            String expResult = testLogicalFile.getLDRI().getName();
            String result = instance.getName();
            assertEquals(expResult, result);
        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.delete();
                ILogicalData load = catalogue.getResourceEntryByLDRI(instance.getPath());
                assertNull(load);
            } catch (CatalogueException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
//    /**
//     * Test of authenticate method, of class WebDataFileResource.
//     */
//    @Test
//    public void testAuthenticate() {
//        System.out.println("authenticate");
//        String user = "";
//        String password = "";
//        WebDataFileResource instance = null;
//        Object expResult = null;
//        Object result = instance.authenticate(user, password);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of authorise method, of class WebDataFileResource.
//     */
//    @Test
//    public void testAuthorise() {
//        System.out.println("authorise");
//        Request request = null;
//        Method method = null;
//        Auth auth = null;
//        WebDataFileResource instance = null;
//        boolean expResult = false;
//        boolean result = instance.authorise(request, method, auth);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRealm method, of class WebDataFileResource.
//     */
//    @Test
//    public void testGetRealm() {
//        System.out.println("getRealm");
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.getRealm();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//

    /**
     * Test of getModifiedDate method, of class WebDataFileResource.
     */
    @Test
    public void testGetModifiedDate() throws Exception {
        WebDataFileResource instance = null;
        try {
            System.out.println("getModifiedDate");
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFile);
            instance = createFileResource(catalogue, testLogicalFile);

            Date result = instance.getModifiedDate();
            assertNotNull(result);

        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.delete();
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Test of getModifiedDate method, of class WebDataFileResource.
     */
    @Test
    public void testGetCreateDate() throws Exception {
        WebDataFileResource instance = null;
        try {
            System.out.println("getModifiedDate");
            LogicalData testLogicalFile = new LogicalData(Path.path("testLogicalFile"), Constants.LOGICAL_FILE);
            catalogue.registerResourceEntry(testLogicalFile);
            instance = createFileResource(catalogue, testLogicalFile);

            Date result = instance.getCreateDate();
            assertNotNull(result);

        } catch (CatalogueException ex) {
            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                instance.delete();
            } catch (NotAuthorizedException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConflictException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadRequestException ex) {
                Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
//
//    /**
//     * Test of checkRedirect method, of class WebDataFileResource.
//     */
//    @Test
//    public void testCheckRedirect() {
//        System.out.println("checkRedirect");
//        Request request = null;
//        WebDataFileResource instance = null;
//        String expResult = "";
//        String result = instance.checkRedirect(request);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    private WebDataDirResource createDirResource(RDMSDLCatalog catalogue, ILogicalData ld) throws IOException, Exception {
        ArrayList<Integer> permArr = new ArrayList<Integer>();
        permArr.add(0);
        permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
        permArr.add(Permissions.REST_ROLE | Permissions.NOACCESS);
        permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
        Metadata meta = ld.getMetadata();
        meta.setPermissionArray(permArr);
        ld.setMetadata(meta);
        catalogue.updateResourceEntry(ld);

        WebDataDirResource collectionResource = new WebDataDirResource(catalogue, ld);
        HttpServletRequest r = new DummyHttpServletRequest();
        WebDavServlet.setThreadlocals(r, null);
        collectionResource.authenticate("user", "pass");

        return collectionResource;
    }

    private WebDataFileResource createFileResource(RDMSDLCatalog catalogue, ILogicalData lf) throws CatalogueException, Exception {
        ArrayList<Integer> permArr = new ArrayList<Integer>();
        permArr.add(0);
        permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
        permArr.add(Permissions.REST_ROLE | Permissions.NOACCESS);
        permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
        Metadata meta = lf.getMetadata();
        meta.setPermissionArray(permArr);
        lf.setMetadata(meta);
        catalogue.updateResourceEntry(lf);
        WebDataFileResource webDAVFile = new WebDataFileResource(catalogue, lf);
        HttpServletRequest r = new DummyHttpServletRequest();
        WebDavServlet.setThreadlocals(r, null);
        webDAVFile.authenticate("user", "pass");

        return webDAVFile;
    }
}

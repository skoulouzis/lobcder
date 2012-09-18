///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package nl.uva.cs.lobcder.webDav.resources;
//
//import com.bradmcevoy.common.Path;
//import com.bradmcevoy.http.Range;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.nio.channels.Channels;
//import java.nio.channels.ReadableByteChannel;
//import java.nio.channels.WritableByteChannel;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.Random;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.servlet.http.HttpServletRequest;
//import nl.uva.cs.lobcder.auth.Permissions;
//import nl.uva.cs.lobcder.catalogue.CatalogueException;
//import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
//import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
//import nl.uva.cs.lobcder.frontend.WebDavServlet;
//import nl.uva.cs.lobcder.resources.*;
//import nl.uva.cs.lobcder.util.Constants;
//import nl.uva.cs.lobcder.util.ConstantsAndSettings;
//import nl.uva.cs.lobcder.util.DummyHttpServletRequest;
//import nl.uva.cs.lobcder.util.LobIOUtils;
//import org.apache.commons.io.IOUtils;
//
//import static org.junit.Assert.*;
//
///**
// *
// * @author S. Koulouzis
// */
//public class TestWebResPerformance {
//
//    private static RDMSDLCatalog catalogue;
//    private static LogicalData testLogicalFile;
//    private static Path testFolderPath;
//    private static LogicalData testLogicalFolder;
//    private static StorageSite site;
//    private static ArrayList<IStorageSite> sites;
//
//    public static void main(String args[]) {
//        try {
//            setUp();
//
////            testCreateNew();
//
//            testSendContent();
////            testCopyIOPerformance();
//        } catch (Exception ex) {
//            Logger.getLogger(TestWebResPerformance.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            System.exit(0);
//        }
//    }
//
//    public static void testCreateNew() throws CatalogueException, Exception {
//        ByteArrayInputStream bais = new ByteArrayInputStream(ConstantsAndSettings.TEST_DATA.getBytes());
//
//        testLogicalFolder.setStorageSites(sites);
//        catalogue.registerResourceEntry(testLogicalFolder);
//
//        ILogicalData loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//        WebDataDirResource instance = createDirResource(catalogue, loaded);
//
//        double N = 2.0;
//        double total = 0;
//        for (int i = 0; i < N; i++) {
//            double start = System.currentTimeMillis();
//            WebDataFileResource result = (WebDataFileResource) instance.createNew(ConstantsAndSettings.TEST_FILE_NAME_1 + i, bais, new Long(ConstantsAndSettings.TEST_DATA.getBytes().length), "text/plain");
//            double end = System.currentTimeMillis();
//            total += (end - start);
//        }
//        double mean = total / N;
//
//        instance.delete();
//        System.out.println("Mean createNew time: " + mean);
//    }
//
//    private static WebDataDirResource createDirResource(IDLCatalogue catalogue, ILogicalData logicalData) throws IOException, Exception {
//        ArrayList<Integer> permArr = new ArrayList<Integer>();
//        permArr.add(0);
//        permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
//        permArr.add(Permissions.REST_ROLE | Permissions.NOACCESS);
//        permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
//        Metadata meta = logicalData.getMetadata();
//        meta.setPermissionArray(permArr);
//        logicalData.setMetadata(meta);
//        catalogue.updateResourceEntry(logicalData);
//
//        WebDataDirResource instance = new WebDataDirResource(catalogue, logicalData);
//
//        HttpServletRequest r = new DummyHttpServletRequest();
//        WebDavServlet.setThreadlocals(r, null);
//        instance.authenticate("user", "pass");
//        return instance;
//    }
//
//    public static void setUp() {
//        try {
//            String confDir = nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR;
//            File propFile = new File(confDir + "/datanucleus.properties");
//            catalogue = new RDMSDLCatalog(propFile);
//
//
//            testLogicalFile = new LogicalData(ConstantsAndSettings.TEST_FILE_PATH_1, Constants.LOGICAL_FILE);
//
//            testFolderPath = Path.path("/WebDataDirResourceTestCollection1");
//            testLogicalFolder = new LogicalData(testFolderPath, Constants.LOGICAL_FOLDER);
//
//            String endpoint = "file:///tmp/";
//            String vphUser = "user1";
//            Credential cred = new Credential(vphUser.split(","));
//            site = new StorageSite(endpoint, cred);
//
//            sites = new ArrayList<IStorageSite>();
//            sites.add(site);
//        } catch (Exception ex) {
//            Logger.getLogger(WebDataFileResourceTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    public static void testSendContent() throws Exception {
//        byte[] testData = new byte[1024 * 1024 * 30];
//        Random r = new Random();
//        r.nextBytes(testData);
//        ByteArrayInputStream bais = new ByteArrayInputStream(testData);
//
//        testLogicalFolder.setStorageSites(sites);
//        catalogue.registerResourceEntry(testLogicalFolder);
//
//        ILogicalData loaded = catalogue.getResourceEntryByLDRI(testLogicalFolder.getLDRI());
//        WebDataDirResource instance = createDirResource(catalogue, loaded);
//
//
//
//        WebDataFileResource file = (WebDataFileResource) instance.createNew(ConstantsAndSettings.TEST_FILE_NAME_1, bais, new Long(testData.length), "text/plain");
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        Map<String, String> params = null;
//        String contentType = "text/plain";
//        double N = 10.0;
//        double total = 0.0;
//        for (int i = 0; i < N; i++) {
//            double start = System.currentTimeMillis();
//            file.sendContent(out, null, params, contentType);
//            double end = System.currentTimeMillis();
//            total += (end - start);
//        }
//        double mean = total / N;
//
//        instance.delete();
//        System.out.println("Mean sendContent time: " + mean);
//    }
//
//    private static void testCopyIOPerformance() throws IOException {
//        byte[] testData = new byte[1024*1024*5];
//        Random r = new Random();
//        r.nextBytes(testData);
//
//        ByteArrayInputStream in = new ByteArrayInputStream(testData);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        double N = 10.0;
//        double total = 0.0;
//        for (int i = 0; i < N; i++) {
//            double start = System.currentTimeMillis();
//            IOUtils.copy(in, out);
//            double end = System.currentTimeMillis();
//            total += (end - start);
//            assertArrayEquals(testData, out.toByteArray());
//        }
//        double mean = total / N;
//        System.out.println("Mean IOUtils.copy time: " + mean);
//
//        total = 0.0;
//        ReadableByteChannel src = Channels.newChannel(in);
//        WritableByteChannel dest = Channels.newChannel(out);
//        for (int i = 0; i < N; i++) {
//            double start = System.currentTimeMillis();
//            LobIOUtils.fastChannelCopy(src, dest);
//            double end = System.currentTimeMillis();
//            total += (end - start);
//            assertArrayEquals(testData, out.toByteArray());
//        }
//        mean = total / N;
//        System.out.println("Mean LobIOUtils.fastChannelCopy time: " + mean);
//
//        total = 0.0;
//        for (int i = 0; i < N; i++) {
//            double start = System.currentTimeMillis();
//            LobIOUtils.copy(in, out);
//            double end = System.currentTimeMillis();
//            total += (end - start);
//            assertArrayEquals(testData, out.toByteArray());
//        }
//        mean = total / N;
//        System.out.println("Mean LobIOUtils.copy time: " + mean);
//    }
//}

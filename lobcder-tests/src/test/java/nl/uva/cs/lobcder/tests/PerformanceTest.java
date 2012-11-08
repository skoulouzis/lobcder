/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author S. koulouzis
 */
public class PerformanceTest {

    private static VDir localTempDir;
    private static URI uri;
    private static String lobcderRoot;
    private static String username;
    private static String password;
    private static HttpClient client;
    private static String lobcdrTestPath;
    private static VFSClient vfsClient;
    public static final int[] FILE_SIZE_IN_KB = {3200};//{100,400,1600,3200};
    public static final int STEP_SIZE_DATASET = 4;
    public static final int MIN_SIZE_DATASET = 10;//3;//640;
    public static final int MAX_SIZE_DATASET = 800;
    public static String measuresPath = "measures";
    private static String hostMeasuresPath;
    private static File downloadDir;
    private static File uploadDir;
    private static String lobcderFilePath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder-tests"
                + File.separator + "etc" + File.separator + "test.proprties";
        Properties prop = TestSettings.getTestProperties(propBasePath);


        initBackendDriver(prop);

        initURL(prop);

        initUsers(prop);

        initClients();

        initLobcderTestDir();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
//        testDriverRemoteDir.delete();
        DeleteMethod del = new DeleteMethod(lobcdrTestPath);
//        debug("Deleteing: "+lobcdrTestPath);
        int status = client.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

        del = new DeleteMethod(lobcderFilePath);
        debug("Deleteing: " + lobcderFilePath);
        status = client.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_MULTI_STATUS);
        
        localTempDir.delete();
    }

    private static void initUsers(Properties prop) {
        username = prop.getProperty("webdav.test.username1");
        password = prop.getProperty("webdav.test.password1");
    }

    private static void initClients() {
        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        client = new HttpClient();

        client.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, password));

    }

    private static void initURL(Properties prop) throws FileNotFoundException {
        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }

        uri = URI.create(testURL);
        lobcderRoot = uri.toASCIIString();
        if (!lobcderRoot.endsWith("/")) {
            lobcderRoot += "/";
        }

        lobcdrTestPath = uri.toString() + "/deleteMe";

        hostMeasuresPath = measuresPath + File.separator + uri.getHost() + File.separator + uri.getPath();
        File measureDir = new File(hostMeasuresPath);
        if (measureDir.mkdirs()) {
            throw new FileNotFoundException("Could not create " + measureDir.getAbsolutePath() + " dir");
        }
        downloadDir = new File(measureDir.getAbsolutePath() + File.separator + "download");
        downloadDir.mkdirs();
        uploadDir = new File(measureDir.getAbsolutePath() + File.separator + "upload");
        uploadDir.mkdirs();
    }

    private static void initBackendDriver(Properties prop) throws MalformedURLException, Exception {
        GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);

        VRS.getRegistry().addVRSDriverClass(
                nl.uva.vlet.vfs.cloud.CloudFSFactory.class);

        Global.init();

        vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();

        VRL vrl = new VRL(prop.getProperty(TestSettings.BACKEND_ENDPOINT));
        ServerInfo info = context.getServerInfoFor(vrl, true);


        info.setUsername(prop.getProperty(TestSettings.BACKEND_USERNAME));
        info.setPassword(prop.getProperty(TestSettings.BACKEND_PASSWORD));

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);
        info.store();

        localTempDir = vfsClient.createDir(new VRL("file:/tmp/testDir"), true);
    }

    private static void initLobcderTestDir() throws IOException {
        MkColMethod mkcol = new MkColMethod(lobcdrTestPath);
        client.executeMethod(mkcol);
        assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void benchmarkTest() throws FileNotFoundException, IOException, InterruptedException, VlException {
        benchmarkUpload();
        benchmarkDownload();
//        uploadOneLargeFile(30000);
//        downloadOneLargeFile();
    }

    private void benchmarkDownload() throws IOException, VlException {

        VFile localFile = localTempDir.createFile("test1MBUpload");

        double sum = 0;
        double start_time = 0;
        double lobcderUpSpeed;

        lobcderFilePath = lobcdrTestPath + localFile.getName();
        GetMethod get = new GetMethod(lobcderFilePath);

        String header = "numOfFiles,sizeDownloaded(kb),DownloadTime(msec),Speed(kb/msec)";
        for (int k : FILE_SIZE_IN_KB) {
            FileWriter writer = new FileWriter(downloadDir.getAbsolutePath() + File.separator + System.currentTimeMillis() + "_" + k + "k.csv");
            writer.append(header + "\n");
            int len = 1024 * k;
            Random generator = new Random();
            byte buffer[] = new byte[len];
            generator.nextBytes(buffer);
            localFile.streamWrite(buffer, 0, buffer.length);

            //just in case delete it first 
//            DeleteMethod del = new DeleteMethod(lobcderFilePath);
//            client.executeMethod(del);

//            PutMethod put = new PutMethod(lobcderFilePath);
//            RequestEntity requestEntity = new InputStreamRequestEntity(localFile.getInputStream());
//            put.setRequestEntity(requestEntity);
//            client.executeMethod(put);

            debug("download file size: " + localFile.getLength());

            for (int i = MIN_SIZE_DATASET; i < MAX_SIZE_DATASET; i *= STEP_SIZE_DATASET) {
                client.executeMethod(get);
                long datasetStart = System.currentTimeMillis();
                for (int j = 0; j < i; j++) {
                    start_time = System.currentTimeMillis();
                    lobcderFilePath = lobcdrTestPath + localFile.getName() + i;
                    get = new GetMethod(lobcderFilePath);
                    int status = client.executeMethod(get);
                    InputStream is = get.getResponseBodyAsStream();
                    File downLoadedFile = new File(localTempDir.getPath() + "/downloadFile");
                    FileOutputStream os = new FileOutputStream(downLoadedFile);
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    double total_millis = System.currentTimeMillis() - start_time;
                    os.flush();
                    os.close();
//                    assertEquals(HttpStatus.SC_OK, status);
                    lobcderUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//                    debug("lobcder download speed=" + lobcderUpSpeed + "KB/s");
//            sum += lobcderUpSpeed;
                }
                long datasetElapsed = System.currentTimeMillis() - datasetStart;
                double sizeDownloaded = i * localFile.getLength();
                double sizeDownloadedKb = (sizeDownloaded / 1024.0);
                double datasetUploadSpeedKBperSec = sizeDownloadedKb / (datasetElapsed / 1000.0);
                debug("mean lobcder download speed=" + datasetUploadSpeedKBperSec + "KB/s");
                writer.append(i + "," + sizeDownloadedKb + "," + datasetElapsed + "," + datasetUploadSpeedKBperSec + "\n");
                writer.flush();
            }
            writer.flush();
            writer.close();

//        double mean = sum / N;
//        debug("mean lobcder upload speed=" + mean + "KB/s");
        }
    }

    private void benchmarkUpload() throws IOException, VlException {
        VFile localFile = localTempDir.createFile("test1MBUpload");

        double sum = 0;
        double start_time = 0;
        double lobcderUpSpeed;

        String lobcderFilePath = lobcdrTestPath + localFile.getName();
        PutMethod put = new PutMethod(lobcderFilePath);
        RequestEntity requestEntity = new FileRequestEntity(new File(localFile.getVRL().toURI()), "application/octet-stream");
        put.setRequestEntity(requestEntity);

        String header = "numOfFiles,sizeUploaded(kb),UploadTime(msec),Speed(kb/msec)";
        for (int k : FILE_SIZE_IN_KB) {
            FileWriter writer = new FileWriter(uploadDir.getAbsolutePath() + File.separator + System.currentTimeMillis() + "_" + k + "k.csv");
            writer.append(header + "\n");
            int len = 1024 * k;
            Random generator = new Random();
            byte buffer[] = new byte[len];
            generator.nextBytes(buffer);
            localFile.streamWrite(buffer, 0, buffer.length);

            debug("upload file size: " + localFile.getLength());

            for (int i = MIN_SIZE_DATASET; i < MAX_SIZE_DATASET; i *= STEP_SIZE_DATASET) {
                debug("Doing dataset: " + i);
                client.executeMethod(put);
                long datasetStart = System.currentTimeMillis();
                for (int j = 0; j < i; j++) {

                    lobcderFilePath = lobcdrTestPath + localFile.getName() + i;
                    put = new PutMethod(lobcderFilePath);
                    requestEntity = new InputStreamRequestEntity(localFile.getInputStream());
                    put.setRequestEntity(requestEntity);
                    start_time = System.currentTimeMillis();
                    int status = client.executeMethod(put);
                    double total_millis = System.currentTimeMillis() - start_time;
//                    assertEquals(HttpStatus.SC_CREATED, status);
                    lobcderUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//                    debug("lobcder upload speed=" + lobcderUpSpeed + "KB/s");
//            sum += lobcderUpSpeed;
                }
                long datasetElapsed = System.currentTimeMillis() - datasetStart;
                double sizeUploaded = i * localFile.getLength();
                double sizeUploadedKb = (sizeUploaded / 1024.0);
                double datasetUploadSpeedKBperSec = sizeUploadedKb / (datasetElapsed / 1000.0);
                debug("mean lobcder upload speed=" + datasetUploadSpeedKBperSec + "KB/s");
                writer.append(i + "," + sizeUploadedKb + "," + datasetElapsed + "," + datasetUploadSpeedKBperSec + "\n");
                writer.flush();
            }
            writer.flush();
            writer.close();

//        double mean = sum / N;
//        debug("mean lobcder upload speed=" + mean + "KB/s");
        }
    }

    private static void debug(String msg) {
        System.err.println("debug: " + msg);
    }

    private void uploadOneLargeFile(int lenInMb) throws VlException, IOException {


        VFile localFile = localTempDir.createFile("testLargeUpload");

        long start_time = 0;

        Random generator = new Random();
        byte buffer[] = new byte[1024 * 1024];
        OutputStream out = localFile.getOutputStream();
        for (int i = 0; i < lenInMb; i++) {
            generator.nextBytes(buffer);
            out.write(buffer);
        }

        File testUploadFile = new File(localFile.getVRL().toURI());

        Part[] parts = {
            new StringPart("param_name", "value"),
            new FilePart(testUploadFile.getName(), testUploadFile)
        };
        lobcderFilePath = lobcdrTestPath + localFile.getName();
        PutMethod method = new PutMethod(lobcderFilePath);


        MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts, method.getParams());
        method.setRequestEntity(requestEntity);
        debug("upload file size: " + localFile.getLength() / (1024.0 * 1024.0) + "Mb");
        start_time = System.currentTimeMillis();
        int status = client.executeMethod(method);
        long datasetElapsed = System.currentTimeMillis() - start_time;
        assertEquals(HttpStatus.SC_CREATED, status);

        double sizeUploaded = localFile.getLength();
        double sizeUploadedKb = (sizeUploaded / 1024.0);
        double sizeUploadedMB = (sizeUploaded / (1024.0 * 1024.0));

        double datasetUploadSpeedKBperSec = sizeUploadedKb / (datasetElapsed / 1000.0);
        double datasetUploadSpeedMBperSec = sizeUploadedMB / (datasetElapsed / 1000.0);
        debug("lobcder upload speed=" + datasetUploadSpeedMBperSec + "MB/s");

        String header = "numOfFiles,sizeUploaded(kb),UploadTime(msec),Speed(kb/msec)";
        File f = new File(uploadDir.getAbsolutePath() + File.separator + "_" + lenInMb + "Mb.csv");
        FileWriter writer;
        if (!f.exists()) {
            writer = new FileWriter(f);
            writer.append(header + "\n");
            writer.flush();
        } else {
            writer = new FileWriter(f, true);
        }
        writer.append("1," + sizeUploadedKb + "," + datasetElapsed + "," + datasetUploadSpeedKBperSec + "\n");
        writer.flush();
        writer.close();
    }

    private void downloadOneLargeFile() {
//        throw new UnsupportedOperationException("Not yet implemented");
    }
}

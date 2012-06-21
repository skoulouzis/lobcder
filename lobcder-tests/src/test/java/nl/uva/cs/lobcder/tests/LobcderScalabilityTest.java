/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author skoulouz
 */
public class LobcderScalabilityTest {

    private URI uri;
    private String lobcderRoot;
    private String password;
    private String[] usernames;
    private HttpClient[] clients;
    private ArrayList<File> datasets;
    public static final int TEST_UPLOAD = 0;
    public static final int CREATE_DATASET = 1;
    public static final int TEST_DOWNLOAD = 2;
    public static final int DELETE = 3;
    public static final int FILE_SIZE_IN_KB = 200;
    public static final int STEP_SIZE_DATASET = 4;
    public static final int MIN_SIZE_DATASET = 20;
    public static final int MAX_SIZE_DATASET = 100;
    public static final int NUM_OF_CLIENTS = 1;
    public static final String[] meanLables = new String[]{ScaleTest.userMeasureLablesPut[0], ScaleTest.userMeasureLablesPut[1], ScaleTest.userMeasureLablesPut[2], ScaleTest.userMeasureLablesPut[3], ScaleTest.userMeasureLablesPut[4], ScaleTest.userMeasureLablesPut[5], "NumOfUsers"};
    public static String measuresPath = "measures";
    private String hostMeasuresPath;

    @Before
    public void setUp() throws Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder-tests"
                + File.separator + "etc" + File.separator + "test.proprties";
        Properties prop = TestSettings.getTestProperties(propBasePath);

        initURL(prop);

        initUsers(prop);

        initClients();

        initDatasets();
    }

    private void initURL(Properties prop) throws FileNotFoundException {
        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }

        this.uri = URI.create(testURL);
        this.lobcderRoot = this.uri.toASCIIString();
        if (!this.lobcderRoot.endsWith("/")) {
            this.lobcderRoot += "/";
        }

        hostMeasuresPath = measuresPath + File.separator + uri.getHost() + File.separator + uri.getPath();
        File measureDir = new File(hostMeasuresPath);
        if (measureDir.mkdirs()) {
            throw new FileNotFoundException("Could not create " + measureDir.getAbsolutePath() + " dir");
        }
    }

    private void initUsers(Properties prop) {
        usernames = prop.getProperty("lobcder.scale.usernames").split(",");
        password = prop.getProperty("lobcder.scale.password");
    }

    private void initClients() {
        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        clients = new HttpClient[usernames.length];

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new HttpClient();

            this.clients[i].getState().setCredentials(
                    new AuthScope(this.uri.getHost(), this.uri.getPort()),
                    new UsernamePasswordCredentials(this.usernames[i], this.password));
        }
    }

    private void initDatasets() throws IOException, InterruptedException {
        //Init a synthetic datasets from 50MB to 4GB
        String dirPath = System.getProperty("java.io.tmpdir") + "/testDatasets";
        File dataSetFolderBase = new File(dirPath);
        if (!dataSetFolderBase.exists()) {
            if (!dataSetFolderBase.mkdirs()) {
                throw new IOException("Faild to create tmp dir");
            }
        }

        datasets = new ArrayList<File>();
        createFiles(32, MIN_SIZE_DATASET, MAX_SIZE_DATASET, dataSetFolderBase);
    }

    public void benchmarkDownload() throws FileNotFoundException, IOException, InterruptedException {
        String path = hostMeasuresPath + File.separator + "download";
        new File(path).mkdir();

        //Create the folder stucture 
        File dataset = datasets.get(0);
        String[] parts = dataset.getAbsolutePath().split("/");
        String testDatasetPath = this.lobcderRoot;
        for (int i = 1; i < parts.length - 1; i++) {
            if (testDatasetPath.endsWith("/")) {
                testDatasetPath += parts[i];
            } else {
                testDatasetPath += "/" + parts[i];
            }
//            System.out.println("MkCol: " + testDatasetPath);
        }

        for (int i = 1; i <= NUM_OF_CLIENTS; i++) {
            runTest(testDatasetPath, i, path, TEST_DOWNLOAD);
            measureMean(path);
        }
    }

    private void benchmarkUpload() throws IOException, InterruptedException {
        try {
            String path = hostMeasuresPath + File.separator + "upload";
            new File(path).mkdir();

            //Create the folder stucture 
            File dataset = datasets.get(0);
            String[] parts = dataset.getAbsolutePath().split("/");
            String testDatasetPath = this.lobcderRoot;
            for (int i = 1; i < parts.length - 1; i++) {
                if (testDatasetPath.endsWith("/")) {
                    testDatasetPath += parts[i];
                } else {
                    testDatasetPath += "/" + parts[i];
                }
//                System.out.println("MkCol: " + testDatasetPath);
                MkColMethod mkcol = new MkColMethod(testDatasetPath);
                clients[0].executeMethod(mkcol);
                assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
            }
            for (int i = 1; i <= NUM_OF_CLIENTS; i++) {//this.clients.length; i++) {
                runTest(testDatasetPath, i, path, TEST_UPLOAD);
                measureMean(path);
                System.out.println("Running client " + i);
//                runTest(testDatasetPath, i, path, DELETE);
            }

        } finally {
        }
    }

    @Test
    public void benchmarkTest() throws FileNotFoundException, IOException, InterruptedException {
        benchmarkUpload();

        benchmarkDownload();

    }

    private void createFiles(int threads, int start, int end, File dataSetFolderBase) throws IOException, InterruptedException {
        ExecutorService execSvc = Executors.newFixedThreadPool(threads);
        long startTime = System.currentTimeMillis();
        for (int i = start; i < end; i *= STEP_SIZE_DATASET) {
            String datasetName = String.valueOf(i);
            String datasetPath = dataSetFolderBase.getAbsolutePath() + "/" + datasetName;
            File dataset = new File(datasetPath);
            if (!dataset.exists()) {
                if (!dataset.mkdirs()) {
                    throw new IOException("Faild to create tmp dir");
                }
            }
            datasets.add(dataset);
            for (int j = 0; j < i; j++) {
                ScaleTest u = new ScaleTest(CREATE_DATASET);
                u.setDataset(dataset);
                u.setFileID(j);
                execSvc.execute(u);
            }
        }
        execSvc.shutdown();
        execSvc.awaitTermination(30, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                delete(c);
            }
        }
        if (!f.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + f);
        }
    }

    private void runTest(String testDatasetPath, int threads, String path, int OP) throws IOException, InterruptedException {
        ExecutorService execSvc = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            ScaleTest user = new ScaleTest(OP);
            user.setClient(clients[i]);
            user.setDataset(datasets);
            user.setLobcderURL(lobcderRoot);
            user.setWorkingPath(testDatasetPath);
            user.setSaveMeasurePath(path);
            execSvc.execute(user);
        }
        execSvc.shutdown();
        execSvc.awaitTermination(30, TimeUnit.MINUTES);
    }

    public static long getDirSize(File dir) {
        long size = 0;
        if (dir.isFile()) {
            size = dir.length();
        } else {
            File[] subFiles = dir.listFiles();

            for (File file : subFiles) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getDirSize(file);
                }

            }
        }

        return size;
    }

    private void measureMean(String path) throws FileNotFoundException, IOException {
        File measureDir = new File(path);
        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().contains("scaleUser")) {
                    return true;
                }
                return false;
            }
        };

        File[] measureFiles = measureDir.listFiles(filter);
        String line;
        int universalLine = 0;
        double[][] res = new double[datasets.size() + 1][meanLables.length];
        for (File f : measureFiles) {
            BufferedReader bufRdr = new BufferedReader(new FileReader(f));
//            System.out.println("Open: " + f.getName());
            int fileLineNumber = 0;
            while ((line = bufRdr.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                int fileColumnNumber = 0;
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (fileLineNumber > 0) {
                        res[fileLineNumber][fileColumnNumber] += (Double.valueOf(token)) / measureFiles.length;
                    }
                    fileColumnNumber++;
                }
                fileLineNumber++;
            }
            universalLine++;
        }

        File f = new File(path + File.separator + "meanMeasures" + ".csv");
        FileWriter writer;
        if (!f.exists()) {
            writer = new FileWriter(f);
            for (String l : meanLables) {
                writer.append(l);
                writer.append(",");
            }
            writer.append("\n");
        } else {
            writer = new FileWriter(f, true);
        }

        for (int i = 1; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {

                if (j == 6) {
                    res[i][6] = measureFiles.length;
                }
//                System.out.println(meanLables[j] + "res[" + i + "][" + j + "]" + ":\t\t\t\t\t" + res[i][j]);
////                System.out.println("res[" + i + "][" + j + "] : " + res[i][j]);
//                System.out.print(res[i][j] + ",");
                writer.append(String.valueOf(res[i][j]));
                writer.append(",");
            }
//            System.out.print("\n");
            writer.append("\n");
        }

        writer.flush();
        writer.close();
    }

    private void delete(HttpClient client1, String testFileURI1) throws IOException {
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }

    private static class ScaleTest implements Runnable {

        private ArrayList<File> datasets;
        private String lobcderRoot;
        private HttpClient client;
        private String username;
        private String testDatasetPath;
        private final int op;
        private File dataset;
        private int fileID;
        private FileWriter writer;
        public static String[] userMeasureLablesPut = new String[]{"DatasetID", "sizeUploaded(kb)", "UploadTime(msec)", "Speed(kb/msec)", "putWaitTotalTime(msec)", "putAverageTime(msec)"};
        public static String[] userMeasureLablesGet = new String[]{"DatasetID", "sizeDownloaded(kb)", "DownloadTime(msec)", "Speed(kb/msec)", "getWaitTotalTime(msec)", "getAverageTime(msec)"};
        private String path;

        private ScaleTest(int op) {
            this.op = op;
        }

        @Override
        public void run() {
            try {
                switch (op) {
                    case TEST_UPLOAD:
                        initWorkingURL();
                        upload(path);
                        break;
                    case CREATE_DATASET:
                        createDataset(FILE_SIZE_IN_KB);
                        break;
                    case TEST_DOWNLOAD:
                        download();
                        break;
                    case DELETE:
                        delete(testDatasetPath += "/" + username);
                        break;
                    default:
                        break;
                }

            } catch (Exception ex) {
                Logger.getLogger(LobcderScalabilityTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void upload(String path) throws IOException {
            writer = new FileWriter(path + File.separator + username + ".csv");
            for (String l : userMeasureLablesPut) {
                writer.append(l);
                writer.append(",");
            }
            writer.append("\n");
            for (File d : datasets) {
                File[] files = d.listFiles();
                String datasetName = files[0].getParentFile().getName();
                String path1 = testDatasetPath + "/" + datasetName;
//                debug("Mkol: " + path1);
                MkColMethod mkcol = new MkColMethod(path1);
                client.executeMethod(mkcol);
                double startUpload = System.currentTimeMillis();
                double bytesUploaded = 0;
                double putWaitTotalTime = 0;
                assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
                for (File f : files) {
                    String path2 = path1 + "/" + f.getName();
//                    debug("PUT: " + path2);
                    PutMethod put = new PutMethod(path2);
                    RequestEntity requestEntity = new InputStreamRequestEntity(
                            new FileInputStream(f));
                    put.setRequestEntity(requestEntity);
                    double putStart = System.currentTimeMillis();
                    client.executeMethod(put);
                    double putEnd = System.currentTimeMillis();
                    bytesUploaded += f.length();
                    putWaitTotalTime += (putEnd - putStart);
                    assertEquals(HttpStatus.SC_CREATED, put.getStatusCode());
                }
                double putAverageTime = (double) (putWaitTotalTime / files.length);
                double endUpload = System.currentTimeMillis();
                double elapsedUploadTime = endUpload - startUpload;
                double kBytesUploaded = (double) (bytesUploaded / 1024.0);
                double speed = (double) (kBytesUploaded / elapsedUploadTime);
                writer.append(datasetName + "," + kBytesUploaded + "," + elapsedUploadTime + "," + speed + "," + putWaitTotalTime + "," + putAverageTime + "\n");
            }
            writer.flush();
            writer.close();
        }

        private void download() throws IOException {
            writer = new FileWriter(path + File.separator + username + ".csv");
            for (String l : userMeasureLablesGet) {
                writer.append(l);
                writer.append(",");
            }
            writer.append("\n");
            byte[] buf = new byte[1024 * FILE_SIZE_IN_KB];
            OutputStream os;
            for (File d : datasets) {
                File[] files = d.listFiles();
                String datasetName = files[0].getParentFile().getName();
                String path1 = testDatasetPath + "/" + username + "/" + datasetName;
//                debug("GET: " + path1);


                double startDownload = System.currentTimeMillis();
                double bytesDownloaded = 0;
                double getWaitTotalTime = 0;

                for (File f : files) {
                    String path2 = path1 + "/" + f.getName();
//                    debug("GET: " + path2);
                    GetMethod get = new GetMethod(path2);
                    double putStart = System.currentTimeMillis();
                    client.executeMethod(get);
                    InputStream is = get.getResponseBodyAsStream();
                    int read;
                    File downLoadedFile = new File(System.getProperty("java.io.tmpdir") + File.separator +"testDatasets" +File.separator+f.getName());
                    os = new FileOutputStream(downLoadedFile);
                    while ((read = is.read(buf)) != -1) {
                        os.write(buf, 0, read);
                    }
                    double putEnd = System.currentTimeMillis();
                    bytesDownloaded += downLoadedFile.length();
                    getWaitTotalTime += (putEnd - putStart);

                    assertEquals(HttpStatus.SC_OK, get.getStatusCode());
                }
                double getAverageTime = (double) (getWaitTotalTime / files.length);
                double endDownload = System.currentTimeMillis();
                double elapsedDownloadTime = endDownload - startDownload;
                double kBytesDownloaded = (double) (bytesDownloaded / 1024.0);
                double speed = (double) (kBytesDownloaded / elapsedDownloadTime);
                writer.append(datasetName + "," + kBytesDownloaded + "," + elapsedDownloadTime + "," + speed + "," + getWaitTotalTime + "," + getAverageTime + "\n");
            }
            writer.flush();
            writer.close();
        }

        public void delete(String path) throws IOException {
//            writer = new FileWriter(path + File.separator + username + "delete.csv");
//            for (String l : userDeleteMeasureLables) {
//                writer.append(l);
//                writer.append(",");
//            }
//            writer.append("\n");

            DeleteMethod delete = new DeleteMethod(path);
            client.executeMethod(delete);
            double startDelete = System.currentTimeMillis();

            assertTrue("status: " + delete.getStatusCode(), delete.getStatusCode() == HttpStatus.SC_NO_CONTENT);

            double endDelete = System.currentTimeMillis();
            double elapsedDeleteTime = endDelete - startDelete;
//            writer.append(deletePath + "," + userDeleteMeasureLables +"\n");
//            writer.flush();
//            writer.close();
        }

        private void setDataset(ArrayList<File> datasets) {
            this.datasets = datasets;
        }

        private void setLobcderURL(String lobcderRoot) {
            this.lobcderRoot = lobcderRoot;
        }

        private void debug(String msg) {
            System.err.println(this.getClass().getSimpleName() + "." + username + ":" + msg);
        }

        private void setWorkingPath(String testDatasetPath) {
            this.testDatasetPath = testDatasetPath;
        }

        private void initWorkingURL() throws IOException {
            testDatasetPath += "/" + username;
            MkColMethod mkcol = new MkColMethod(testDatasetPath);
            client.executeMethod(mkcol);
            assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
        }

        private void setClient(HttpClient httpClient) {
            this.client = httpClient;

            Credentials cred = client.getState().getCredentials(AuthScope.ANY);
            this.username = cred.toString().split(":")[0];
        }

        private void createDataset(int sizeInk) throws FileNotFoundException, IOException {
            byte[] data = new byte[1024 * sizeInk];//1MB
            Random r = new Random();
            File f = new File(dataset.getAbsolutePath() + "/file" + this.fileID);
            if (!f.exists()) {
//                debug("Writing: " + f.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(f);
                r.nextBytes(data);
                fos.write(data);
                fos.flush();
                fos.close();
            }
        }

        private void setDataset(File dataset) {
            this.dataset = dataset;
        }

        private void setFileID(int j) {
            this.fileID = j;
        }

        private void setSaveMeasurePath(String path) {
            this.path = path;
        }
    }
}

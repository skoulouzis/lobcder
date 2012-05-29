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
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
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

    private void initURL(Properties prop) {
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
        int start = 20;
        int end = 100;
        String dirPath = System.getProperty("java.io.tmpdir") + "/testDatasets";
        File dataSetFolderBase = new File(dirPath);
        if (!dataSetFolderBase.exists()) {
            if (!dataSetFolderBase.mkdirs()) {
                throw new IOException("Faild to create tmp dir");
            }
        }

        datasets = new ArrayList<File>();
        createFiles(32, start, end, dataSetFolderBase);
    }

    @Test
    public void testUpload() throws FileNotFoundException, IOException, InterruptedException {
        try {
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
                System.out.println("MkCol: " + testDatasetPath);
                MkColMethod mkcol = new MkColMethod(testDatasetPath);
                clients[0].executeMethod(mkcol);
                assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
            }
            
            upload(testDatasetPath, 5);
        } finally {
        }
    }

    private void createFiles(int threads, int start, int end, File dataSetFolderBase) throws IOException, InterruptedException {
        ExecutorService execSvc = Executors.newFixedThreadPool(threads);
        long startTime = System.currentTimeMillis();
        for (int i = start; i < end; i *= 4) {
            String datasetName = "dataset" + i + "MB";
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
        System.out.println(threads + " , " + (endTime - startTime));
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

    private void upload(String testDatasetPath, int threads) throws IOException, InterruptedException {

        ExecutorService execSvc = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            ScaleTest user = new ScaleTest(TEST_UPLOAD);
            user.setClient(clients[i]);
            user.setDataset(datasets);
            user.setLobcderURL(lobcderRoot);
            user.setWorkingPath(testDatasetPath);
            execSvc.execute(user);
        }
        execSvc.shutdown();
        execSvc.awaitTermination(30, TimeUnit.MINUTES);
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

        private ScaleTest(int op) {
            this.op = op;
        }

        @Override
        public void run() {
            try {
                switch (op) {
                    case TEST_UPLOAD:
                        initWorkingURL();
                        upload();
                        break;
                    case CREATE_DATASET:
                        createDataset(10);
                    default:
                        break;
                }

            } catch (Exception ex) {
                Logger.getLogger(LobcderScalabilityTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void upload() throws IOException {
            for (File d : datasets) {
                File[] files = d.listFiles();
                String parent = files[0].getParentFile().getName();
                String path1 = testDatasetPath + "/" + parent;
                debug("Mkol: " + path1);
                MkColMethod mkcol = new MkColMethod(path1);
                client.executeMethod(mkcol);
                assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
                for (File f : files) {
                    String path2 = path1 + "/" + f.getName();
                    debug("PUT: " + path2);
                    PutMethod put = new PutMethod(path2);
                    RequestEntity requestEntity = new InputStreamRequestEntity(
                            new FileInputStream(f));
                    put.setRequestEntity(requestEntity);
                    client.executeMethod(put);
                    assertEquals(HttpStatus.SC_CREATED, put.getStatusCode());
                }
            }
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
    }
}

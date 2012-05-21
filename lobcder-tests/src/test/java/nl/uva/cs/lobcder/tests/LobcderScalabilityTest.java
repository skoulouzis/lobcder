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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
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
    private String root;
    private String password;
    private String[] usernames;
    private HttpClient[] clients;
    private ArrayList<File> datasets;

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
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
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

    private void initDatasets() throws IOException {
        //Init a synthetic datasets from 50MB to 4GB
        int start = 14;
        int end = 100;
        String dirPath = System.getProperty("java.io.tmpdir") + "/testDatasets";
        File dataSetFolderBase = new File(dirPath);
        if (!dataSetFolderBase.exists()) {
            if (!dataSetFolderBase.mkdirs()) {
                throw new IOException("Faild to create tmp dir");
            }
        }
        byte[] data = new byte[1024 * 100];//1MB
        Random r = new Random();
        
        String datasetName;
        String datasetPath;
        datasets = new ArrayList<File>();
        FileOutputStream fos;
        for (int i = start; i < end; i *= 2) {
            datasetName = "dataset" + i + "MB";
            datasetPath = dataSetFolderBase.getAbsolutePath() + "/" + datasetName;
            File dataset = new File(datasetPath);
            if (!dataset.exists()) {
                if (!dataset.mkdirs()) {
                    throw new IOException("Faild to create tmp dir");
                }
            }
            datasets.add(dataset);
            for (int j = 0; j < i; j++) {
                File f = new File(dataset.getAbsolutePath() + "/file" + j);
                if (!f.exists()) {
                    fos = new FileOutputStream(f);
                    r.nextBytes(data);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                }
            }
        }
    }

    @Test
    public void testUpload() throws FileNotFoundException, IOException {
        try {
            File dataset = datasets.get(0);
            String[] parts = dataset.getAbsolutePath().split("/");
            String testDatasetPath = this.root;
            for (int i = 1; i < parts.length; i++) {
                if (testDatasetPath.endsWith("/")) {
                    testDatasetPath += parts[i];
                } else {
                    testDatasetPath += "/" + parts[i];
                }
                System.out.println("----> " + testDatasetPath);
                MkColMethod mkcol = new MkColMethod(testDatasetPath);
                clients[0].executeMethod(mkcol);
                assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
            }

            File[] files = dataset.listFiles();
            for (File f : files) {
                String path = testDatasetPath + "/" + f.getName();
                System.out.println("PUT: " + path);
                PutMethod put = new PutMethod(path);
                RequestEntity requestEntity = new InputStreamRequestEntity(
                        new FileInputStream(f));
                put.setRequestEntity(requestEntity);
                clients[0].executeMethod(put);
                assertEquals(HttpStatus.SC_CREATED, put.getStatusCode());
                
            }

            DeleteMethod delete = new DeleteMethod(testDatasetPath+"/");
            clients[0].executeMethod(delete);
            System.out.println(delete.getStatusCode() + " " + delete.getStatusText());

        } finally {
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

/**
 *
 * @author S. koulouzis
 */
public class TestREST {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;
    private String testres1;
    private String testres2;
    private String testcol;
    private String restURL;

    @Before
    public void setUp() throws Exception {
//        String propBasePath = System.getProperty("user.home") + File.separator
//                + "workspace" + File.separator + "lobcder-tests"
//                + File.separator + "etc" + File.separator + "test.proprties";
        String propBasePath = "etc" + File.separator + "test.proprties";

        Properties prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-2.0-SNAPSHOT/";
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

        this.username = prop.getProperty(("webdav.test.username1"), "");
        if (username == null) {
            username = "user";
        }
        assertTrue(username != null);
        this.password = prop.getProperty(("webdav.test.password1"), "");
        if (password == null) {
            password = "token0";
        }
        assertTrue(password != null);

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }

        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));

        restURL = prop.getProperty(("rest.test.url"), "http://localhost:8080/lobcder-2.0-SNAPSHOT/rest/");


        testcol = this.root + "testResourceId/";

    }

    @After
    public void tearDown() throws Exception {
    }

    private void createCollection() throws IOException {
        MkColMethod mkcol = new MkColMethod(testcol);
        int status = this.client.executeMethod(mkcol);
        assertEquals(HttpStatus.SC_CREATED, status);


        PutMethod put = new PutMethod(this.root + "testResourceId/file1");
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        status = this.client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        put = new PutMethod(this.root + "testResourceId/file2");
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        status = this.client.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);
    }

    private void deleteCollection() throws IOException {
        DeleteMethod delete = new DeleteMethod(testcol);
        int status = this.client.executeMethod(delete);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSetGetSupervised() throws IOException {
        try {
            createCollection();
            WebResource webResource;

            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            Client client2 = Client.create(clientConfig);
            webResource = client2.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");

            ClientResponse response = webResource.path("Items").path("TRUE").
                    queryParams(params).put(ClientResponse.class);
            assertTrue("status: " + response.getStatus(), response.getStatus() == HttpStatus.SC_OK || response.getStatus() == HttpStatus.SC_NO_CONTENT);

            params = new MultivaluedMapImpl();
            params.add("path", "/testResourceId");
//            List<LogicalData> use = webResource.path("Items").
//                    accept(MediaType.APPLICATION_XML).
//                    get(new GenericType<List<LogicalData>>() {
//            });


            response = webResource.path("Items").
                    accept(MediaType.APPLICATION_XML).
                    get(ClientResponse.class);
            String entry = response.getEntity(String.class);
            System.out.println(entry);

        } finally {
//            deleteCollection();
        }
    }

    public class DataItemsResponse {

        public LogicalData[] category;

        public DataItemsResponse() {
        }
    }

    public class LogicalData {
    }
}

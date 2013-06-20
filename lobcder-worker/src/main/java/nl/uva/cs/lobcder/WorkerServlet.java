/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import io.milton.http.Range;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import org.apache.commons.httpclient.HttpStatus;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WorkerServlet extends HttpServlet {

    private Client restClient;
    private String restURL;
    private final String username;
    private final String password;
    private Map<String, Long> weightPDRIMap;
    private long size;

    public WorkerServlet() throws FileNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/auth.properties");

//        String propBasePath = File.separator + "test.proprties";
        Properties prop = Util.getTestProperties(in);

        restURL = prop.getProperty(("rest.url"), "http://localhost:8080/lobcder/rest/");
        username = prop.getProperty(("rest.username"), "user");
        password = prop.getProperty(("rest.password"), "pass");


        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        restClient = Client.create(clientConfig);
        restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username, password));

        weightPDRIMap = new HashMap<>();
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String filePath = request.getPathInfo();
        if (filePath.length() > 1) {
            long start = System.currentTimeMillis();
            OutputStream out = response.getOutputStream();
            PDRI pdri = getPDRI(filePath);
            String rangeStr = request.getHeader(Constants.RANGE_HEADER_NAME);
            if (rangeStr != null) {
                Range range = Range.parse(rangeStr.split("=")[1]);
                pdri.copyRange(range, out);
                response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
            } else {
                trasfer(pdri, out);
            }
            long elapsed = System.currentTimeMillis() - start;
            long elapsedSec = elapsed / 1000;
            if (elapsedSec <= 0) {
                elapsedSec = 1;
            }
            long speed = size / elapsedSec;
            this.weightPDRIMap.put(pdri.getURI(), speed);
        }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "LOBCDER worker";
    }

    private PDRI getPDRI(String filePath) throws IOException {
        WebResource webResource = restClient.resource(restURL);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("path", filePath);

        WebResource res = webResource.path("items").path("query").queryParams(params);

        List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                get(new GenericType<List<LogicalDataWrapped>>() {
        });

        PDRIDesc pdriDesc = null;
        for (LogicalDataWrapped ld : list) {
            if (ld != null) {
                Set<PDRIDesc> pdris = ld.pdriList;
                size = ld.logicalData.length;
                if (pdris != null && !pdris.isEmpty()) {
                    pdriDesc = selectBestPDRI(pdris);
                }
            }
        }
        return new VPDRI(pdriDesc.name, pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.ZERO, false);
    }

    private void trasfer(PDRI pdri, OutputStream out) throws IOException {
        try {
            if (!pdri.getEncrypted()) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), pdri.getData(), out);
                cBuff.startTransfer(new Long(-1));
            }
        } catch (VlException ex) {
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.flush();
            out.close();
        }
    }

    private PDRIDesc selectBestPDRI(Set<PDRIDesc> pdris) {
        if (weightPDRIMap.isEmpty()) {
            for (PDRIDesc p : pdris) {
                weightPDRIMap.put(p.resourceUrl, Long.valueOf(10));
            }
        }
        long totalSum = 0;
        for (PDRIDesc p : pdris) {
            Long speed = weightPDRIMap.get(p.resourceUrl);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Speed: : {0}", speed);
            totalSum += speed;

        }
        int itemIndex = new Random().nextInt((int) totalSum);

        long sum = 0;
        int i = 0;
        while (sum < itemIndex) {
            i++;
            String url = pdris.iterator().next().resourceUrl;
            sum = sum + weightPDRIMap.get(url);
        }
        PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
        int index = i - 1;
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, " SELECT: " + array[index].resourceUrl);
        return array[index];
    }

    @XmlRootElement
    public static class PDRIDesc {

        public boolean encrypt;
        public long id;
        public String name;
        public String password;
        public String resourceUrl;
        public String username;
    }

    @XmlRootElement
    public static class LogicalDataWrapped {

        public LogicalData logicalData;
        public String path;
        public Set<PDRIDesc> pdriList;
        public Set<Permissions> permissions;
    }

    @XmlRootElement
    public static class LogicalData {

        public int checksum;
        public String contentTypesAsString;
        public long createDate;
        public long lastValidationDate;
        public long length;
        public int lockTimeout;
        public long modifiedDate;
        public String name;
        public String owner;
        public int parentRef;
        public int pdriGroupId;
        public boolean supervised;
        public String type;
        public int uid;
    }

    @XmlRootElement
    public static class Permissions {

        public String owner;
        public Set<String> read;
        public Set<String> write;
    }
}

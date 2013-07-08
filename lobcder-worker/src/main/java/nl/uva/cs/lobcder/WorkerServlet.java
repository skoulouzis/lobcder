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
import io.milton.common.Path;
import io.milton.http.Range;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.vrl.VRL;
import org.apache.http.HttpStatus;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WorkerServlet extends HttpServlet {

//    private Client restClient;
    private String restURL;
    private Map<String, Long> weightPDRIMap;
    private long size;
    private int numOfTries = 0;
    private long numOfGets;
    private long sleepTime = 2;
    private String token;
    private final DefaultClientConfig clientConfig;

    public WorkerServlet() throws FileNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/auth.properties");

//        String propBasePath = File.separator + "test.proprties";
        Properties prop = Util.getTestProperties(in);

        restURL = prop.getProperty(("rest.url"), "http://localhost:8080/lobcder/rest/");


        clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        weightPDRIMap = new HashMap<String, Long>();
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
            Path pathAndToken = Path.path(filePath);
            token = pathAndToken.getName();
            String path = pathAndToken.getParent().toString();
            try {
                numOfGets++;
                long start = System.currentTimeMillis();

                PDRI pdri = getPDRI(path);
                if (pdri == null) {
                    response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    return;
                } else {
                    OutputStream out = response.getOutputStream();
                    String rangeStr = request.getHeader(Constants.RANGE_HEADER_NAME);
                    if (rangeStr != null) {
                        Range range = Range.parse(rangeStr.split("=")[1]);
                        pdri.copyRange(range, out);
                        response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
                        return;
                    } else {
                        trasfer(pdri, out, false);
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    long elapsedSec = elapsed / 1000;
                    if (elapsedSec <= 0) {
                        elapsedSec = 1;
                    }

                    long speed = size / elapsedSec;
                    Long oldSpeed = weightPDRIMap.get(pdri.getHost());
                    if (oldSpeed == null) {
                        oldSpeed = speed;
                    }
                    long averagre = (speed + oldSpeed) / numOfGets;
                    this.weightPDRIMap.put(pdri.getHost(), averagre);
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Average speed for  : {0} : " + averagre, pdri.getHost());
                }
                numOfTries = 0;
                sleepTime = 2;
            } catch (Exception ex) {
                //Maybe we can look for another pdri
                if (ex.getMessage() != null) {
                    if (ex.getMessage().contains("Resource not found")
                            || ex.getMessage().contains("Could not stat remote")) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                        response.setStatus(HttpStatus.SC_CONFLICT);
                        return;
                    }
                    if (ex.getMessage().contains("returned a response status of 404 Not Found")) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                        response.setStatus(HttpStatus.SC_NOT_FOUND);
                        return;
                    }
                    if (ex.getMessage().contains("returned a response status of 401 Unauthorized")) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                        response.setStatus(HttpStatus.SC_UNAUTHORIZED);
                        return;
//                    throw new IOException(ex);
                    }
                }
                if (numOfTries < Constants.RECONNECT_NTRY) {
                    try {
                        numOfTries++;
                        sleepTime = sleepTime + 2;
                        Thread.sleep(sleepTime);
                        doGet(request, response);
                    } catch (InterruptedException ex1) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex1);
                        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    }
                } else {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                    response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                }
            }
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

    private PDRI getPDRI(String filePath) throws IOException, URISyntaxException {
        PDRIDesc pdriDesc = null;//new PDRIDesc();
        Client restClient = null;
        try {
            restClient = Client.create(clientConfig);

            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-" + InetAddress.getLocalHost().getHostName(), token));

            WebResource webResource = restClient.resource(restURL);
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", filePath);
            WebResource res = webResource.path("items").path("query").queryParams(params);


            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Asking master. Token: {0}", token);

            List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });

            int count = 0;
            for (LogicalDataWrapped ld : list) {
                if (ld != null) {
                    Set<PDRIDesc> pdris = ld.pdriList;
                    size = ld.logicalData.length;
                    if (pdris != null && !pdris.isEmpty()) {
                        pdriDesc = selectBestPDRI(pdris);
                        while (pdriDesc == null) {
                            count++;
                            pdriDesc = selectBestPDRI(pdris);
                            if (count > Constants.RECONNECT_NTRY) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
//            if (ex.getMessage().contains("returned a response status of 401 Unauthorized")
//                    || ex.getMessage().contains("returned a response status of 404 Not Found")) {
//                throw new IOException(ex);
//            }
            if (numOfTries < Constants.RECONNECT_NTRY) {
                try {
                    numOfTries++;
                    sleepTime = sleepTime + 2;
                    Thread.sleep(sleepTime);
                    getPDRI(filePath);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex1);
                    throw new IOException(ex1);
                }
            }
        } finally {
            if (restClient != null) {
                restClient.destroy();
            }
        }
        numOfTries = 0;
        sleepTime = 2;
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selected pdri: {0}", pdriDesc.resourceUrl);
        return new WorkerVPDRI(pdriDesc.name, pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.ZERO, false);
    }

    private void trasfer(PDRI pdri, OutputStream out, boolean withCircularStream) throws IOException {
        InputStream in = null;
        try {
            in = pdri.getData();
            int bufferSize;
            if (pdri.getLength() < Constants.BUF_SIZE) {
                bufferSize = (int) pdri.getLength();
            } else {
                bufferSize = Constants.BUF_SIZE;
            }
            if (!pdri.getEncrypted() && withCircularStream) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((bufferSize), in, out);
                cBuff.startTransfer(new Long(-1));
                cBuff.setStop(withCircularStream);
            } else if (!pdri.getEncrypted() && !withCircularStream) {
                int read;
                byte[] copyBuffer = new byte[bufferSize];
                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }
            }
            numOfTries = 0;
            sleepTime = 2;
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Resource not found")
                    || ex.getMessage().contains("Could not stat remote")) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException(ex.getMessage());
            }
            withCircularStream = false;
//            if (ex instanceof nl.uva.vlet.exception.VlException) {
//                withCircularStream = false;
//            }
            if (numOfTries < Constants.RECONNECT_NTRY) {
                try {
                    numOfTries++;
                    sleepTime = sleepTime + 2;
                    Thread.sleep(sleepTime);
                    trasfer(pdri, out, withCircularStream);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex1);
                    throw new IOException(ex1.getMessage());
                }
            } else {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException(ex.getMessage());
            }
        } finally {
            try {
//                if (out != null) {
//                    out.flush();
//                    out.close();
//                }
                if (in != null) {
                    in.close();
                }
            } catch (java.net.SocketException ex) {
                //
            }
        }
    }

    private PDRIDesc selectBestPDRI(Set<PDRIDesc> pdris) throws URISyntaxException, UnknownHostException {
        if (weightPDRIMap.isEmpty() || weightPDRIMap.size() < pdris.size()) {
            //Just return one at random;
            int index = new Random().nextInt(pdris.size());
            PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
            return array[index];
        }

        long sumOfSpeed = 0;
        for (PDRIDesc p : pdris) {
            URI uri = new URI(p.resourceUrl);
            String host;
            if (uri.getScheme().equals("file")
                    || StringUtil.isEmpty(uri.getHost())
                    || uri.getHost().equals("localhost")
                    || uri.getHost().equals("127.0.0.1")) {
                host = InetAddress.getLocalHost().getHostName();
            } else {
                host = uri.getHost();
            }
            Long speed = weightPDRIMap.get(host);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Speed: : {0}", speed);
            sumOfSpeed += speed;
        }
        int itemIndex = new Random().nextInt((int) sumOfSpeed);

        for (PDRIDesc p : pdris) {
            Long speed = weightPDRIMap.get(new URI(p.resourceUrl).getHost());
            if (itemIndex < speed) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Returning : {0}", p.resourceUrl);
                return p;
            }
            itemIndex -= speed;
        }
//        long sum = 0;
//        int i = 0;
//
//        while (sum < itemIndex) {
//            i++;
//            winner = pdris.iterator().next();
////            sum = sum + weightPDRIMap.get(new URI(winner.resourceUrl).getHost());
//            sum = sum + weightPDRIMap.get(new URI(winner.resourceUrl).getHost());
//        }
//        PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
//        int index;
//        if (i > 0) {
//            index = i - 1;
//        } else {
//            index = i;
//        }

        return null;
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

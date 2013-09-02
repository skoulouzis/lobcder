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
import com.sun.jersey.client.urlconnection.HTTPSProperties;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.extern.java.Log;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import org.apache.http.HttpStatus;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WorkerServlet extends HttpServlet {

//    private Client restClient;
    private String restURL;
    private long size;
    private int numOfTries = 0;
    private long sleepTime = 2;
    private String token;
    private Client restClient;
    private final ClientConfig clientConfig;
    private static final Map<String, Double> weightPDRIMap = new HashMap<String, Double>();
    private static final HashMap<String, Integer> numOfGetsMap = new HashMap<String, Integer>();
    private final Map<String, LogicalDataWrapped> logicalDataCache = new HashMap<String, LogicalDataWrapped>();
//    private final String uname;

    public WorkerServlet() throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/auth.properties");

//        String propBasePath = File.separator + "test.proprties";
        Properties prop = Util.getTestProperties(in);
        in.close();

        restURL = prop.getProperty(("rest.url"), "http://localhost:8080/lobcder/rest/");
//        token = prop.getProperty(("rest.pass"));
//        uname = prop.getProperty(("rest.uname"));
        clientConfig = configureClient();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
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
            String fileUID = pathAndToken.getParent().toString();
            try {
                long start = System.currentTimeMillis();
                Range range = null;
                long startGetPDRI = System.currentTimeMillis();
                PDRI pdri = getPDRI(fileUID);
                long elapsedGetPDRI = startGetPDRI - System.currentTimeMillis();
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "elapsedGetPDRI: " + elapsedGetPDRI);
                if (pdri == null) {
                    response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    return;
                } else {
                    long startTransfer = System.currentTimeMillis();
                    OutputStream out = response.getOutputStream();
                    String rangeStr = request.getHeader(Constants.RANGE_HEADER_NAME);
                    if (rangeStr != null) {
                        range = Range.parse(rangeStr.split("=")[1]);
                        pdri.copyRange(range, out);
//                        response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
                        return;
                    } else {
                        transfer(pdri, out, false);
                    }
                    long elapsedTransfer = startTransfer - System.currentTimeMillis();
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "elapsedTransfer: " + elapsedTransfer);

                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed <= 0) {
                        elapsed = 1;
                    }

                    long len;
                    if (range != null) {
                        len = range.getFinish() - range.getStart() + 1;
                    } else {
                        len = size;
                    }

                    double speed = ((len * 8.0) * 1000.0) / (elapsed * 1000.0);
                    Double oldSpeed = weightPDRIMap.get(pdri.getHost());
                    if (oldSpeed == null) {
                        oldSpeed = speed;
                    }
                    Integer numOfGets = numOfGetsMap.get(pdri.getHost());
                    if (numOfGets == null) {
                        numOfGets = 1;
                    }
                    double averagre = (speed + oldSpeed) / (double) numOfGets;
                    numOfGetsMap.put(pdri.getHost(), numOfGets++);
                    weightPDRIMap.put(pdri.getHost(), averagre);

                    String speedMsg = "Source: " + request.getLocalAddr() + " Destination: " + request.getRemoteAddr() + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + len + " bytes";
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, speedMsg);
                    String averageSpeedMsg = "Average speed: Source: " + pdri.getHost() + " Destination: " + request.getLocalAddr() + " Rx_Speed: " + averagre + " Kbites/sec Rx_Size: " + len + " bytes";
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, averageSpeedMsg);
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
            } finally {
                logicalDataCache.remove(fileUID);
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

    private PDRI getPDRI(String fileUID) throws IOException, URISyntaxException {
        PDRIDesc pdriDesc = null;//new PDRIDesc();
        LogicalDataWrapped logicalData = null;

        try {
            logicalData = logicalDataCache.get(fileUID);
            if (logicalData == null) {
                if (restClient == null) {
                    restClient = Client.create(clientConfig);
                }
                restClient.removeAllFilters();
                restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-" + InetAddress.getLocalHost().getHostName(), token));
//            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(uname, token));

                WebResource webResource = restClient.resource(restURL);
//                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Asking master. Token: {0}", token);
//                long startGetLogicalData = System.currentTimeMillis();

                WebResource res = webResource.path("item").path("query").path(fileUID);
                logicalData = res.accept(MediaType.APPLICATION_XML).
                        get(new GenericType<LogicalDataWrapped>() {
                });
                logicalDataCache.put(fileUID, logicalData);
//                long elapsedGetLogicalData = System.currentTimeMillis() - startGetLogicalData;
//                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "elapsedGetLogicalData: {0}", elapsedGetLogicalData);
            }

            int count = 0;
            if (logicalData != null) {

                Set<PDRIDesc> pdris = logicalData.pdriList;
                size = logicalData.logicalData.length;
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
        } catch (Exception ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("returned a response status of 404 Not Found")) {
//                    || ex.getMessage().contains("returned a response status of 401 Unauthorized")) {
                throw new IOException(ex);
            }
            if (numOfTries < Constants.RECONNECT_NTRY) {
                try {
                    numOfTries++;
                    sleepTime = sleepTime + 2;
                    Thread.sleep(sleepTime);
                    getPDRI(fileUID);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex1);
                    throw new IOException(ex1);
                }
            }
        } finally {
//            if (restClient != null) {
//                restClient.destroy();
//            }
        }
        numOfTries = 0;
        sleepTime = 2;
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selected pdri: {0}", pdriDesc.resourceUrl);


        return new WorkerVPDRI(pdriDesc.name, pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.valueOf(Long.valueOf(pdriDesc.key)), false);
//        return new WorkerVPDRI(pdriDesc.name , pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.ZERO, false);
    }

    private void transfer(PDRI pdri, OutputStream out, boolean withCircularStream) throws IOException {
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
                    transfer(pdri, out, withCircularStream);
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
        if (pdris.size() == 1) {
            return pdris.iterator().next();
        }
        if (weightPDRIMap.isEmpty() || weightPDRIMap.size() < pdris.size()) {
            //Just return one at random;
            int index = new Random().nextInt(pdris.size());
            PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selecting Random: {0}", array[index].resourceUrl);
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
            Double speed = weightPDRIMap.get(host);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Speed: : {0}", speed);
            sumOfSpeed += speed;
        }
        if (sumOfSpeed <= 0) {
            int index = new Random().nextInt(pdris.size());
            PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
            return array[index];
        }
        int itemIndex = new Random().nextInt((int) sumOfSpeed);

        for (PDRIDesc p : pdris) {
            Double speed = weightPDRIMap.get(new URI(p.resourceUrl).getHost());
            if (speed == null) {
                speed = Double.valueOf(0);
            }
            if (itemIndex < speed) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selecting:{0}  with speed: {1}", new Object[]{p.resourceUrl, speed});
                return p;
            }
            itemIndex -= speed;
        }

        int index = new Random().nextInt(pdris.size());
        PDRIDesc[] array = pdris.toArray(new PDRIDesc[pdris.size()]);
        return array[index];
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

    @XmlRootElement
    public static class PDRIDesc {

        public boolean encrypt;
        public long id;
        public String key;
        public String name;
        public String password;
        public String pdriGroupRef;
        public String resourceUrl;
        public String username;
    }

    public ClientConfig configureClient() {
        TrustManager[] certs = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }
            }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        ClientConfig config = new DefaultClientConfig();
        try {
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    },
                    ctx));
        } catch (Exception e) {
        }
        return config;
    }
}

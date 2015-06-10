/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import io.milton.common.Path;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.extern.java.Log;
import static nl.uva.cs.lobcder.Util.ChacheEvictionAlgorithm.LRU;
import static nl.uva.cs.lobcder.Util.ChacheEvictionAlgorithm.MRU;
import nl.uva.cs.lobcder.auth.AuthTicket;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.cs.lobcder.rest.Endpoints;
import nl.uva.cs.lobcder.rest.wrappers.LogicalDataWrapped;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.TeeOutputStream;
import org.globus.tools.ui.util.UITools;

/**
 * A file servlet supporting resume of downloads and client-side caching and
 * GZIP of text content. This servlet can also be used for images, client-side
 * caching would become more efficient. This servlet can also be used for text
 * files, GZIP would decrease network bandwidth.
 *
 * @author BalusC, S. Koulouzis Based on the code on
 * http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
@Log
public final class WorkerServlet extends HttpServlet {

    // Constants ----------------------------------------------------------------------------------
//    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    // Properties ---------------------------------------------------------------------------------
    private Integer bufferSize;
    private Boolean setResponseBufferSize;
//    private Boolean useCircularBuffer;
    private String restURL;
    private String token;
    private ClientConfig clientConfig;
    private String fileUID;
    private final Map<String, LogicalDataWrapped> logicalDataCache = new HashMap<>();
    private final Map<String, Long> fileAccessMap = new HashMap<>();
    private Client restClient;
//    private long sleepTime = 2;
    private static final Map<String, Double> weightPDRIMap = new HashMap<>();
    private static final HashMap<String, Integer> numOfGetsMap = new HashMap<>();
    private InputStream in;
    private int responseBufferSize;
//    private double lim = 4;
    private boolean qosCopy;
    private int warnings;
    private double progressThresshold;
    private double coefficient;
    private String fileLogicalName;
    private File cacheDir;
    private File cacheFile;
    private boolean sendStats;
    private WebResource webResource;

    // Actions ------------------------------------------------------------------------------------
    /**
     * Initialize the servlet.
     *
     * @see HttpServlet#init().
     */
    @Override
    public void init() throws ServletException {
        try {
            setResponseBufferSize = Util.isResponseBufferSize();
//            useCircularBuffer = Util.isCircularBuffer();
            qosCopy = Util.doQosCopy();
            bufferSize = Util.getBufferSize();

            restURL = Util.getRestURL();
            token = Util.getRestPassword();
            sendStats = Util.sendStats();
//            lim = Util.getRateOfChangeLim();
            //        uname = prop.getProperty(("rest.uname"));
            clientConfig = configureClient();
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            warnings = Util.getNumOfWarnings();
            progressThresshold = Util.getProgressThresshold();
            coefficient = Util.getProgressThressholdCoefficient();
            cacheDir = new File(System.getProperty("java.io.tmpdir") + File.separator + Util.getBackendWorkingFolderName());
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            getPDRIs();
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    /**
//     * Process HEAD request. This returns the same headers as GET request, but
//     * without content.
//     *
//     * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
//     */
//    @Override
//    protected void doHead(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//        try {
//            // Process request without content.
//            processRequest(request, response, false);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (URISyntaxException ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    /**
     * Process GET request.
     *
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UnsupportedEncodingException {
        try {
            // Process request with content.
//            authenticate(request, response);
            processRequest(request, response, true);
        } catch (IOException | InterruptedException | URISyntaxException | VlException | JAXBException ex) {
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
            handleError(ex, response);
//        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InvalidKeySpecException ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InvalidKeyException ex) {
//            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Process the actual request.
     *
     * @param request The request to be processed.
     * @param response The response to be created.
     * @param content Whether the request body should be written (GET) or not
     * (HEAD).
     * @throws IOException If something fails at I/O level.
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content)
            throws IOException, InterruptedException, URISyntaxException, VlException, JAXBException {
        long startTime = System.currentTimeMillis();

        // Get requested file by path info.
        String requestedFile = request.getPathInfo();
        // Check if file is actually supplied to the request URL.

        PDRI pdri = null;
        if (requestedFile == null || requestedFile.split("/").length < 2) {
            // Do your thing if the file is not supplied to the request URL.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path pathAndToken = Path.path(requestedFile);
//            token = pathAndToken.getName();
        fileUID = pathAndToken.getParent().toString().replaceAll("/", "");
//        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "token: {0} fileUID: {1}", new Object[]{token, fileUID});

        long startGetPDRI = System.currentTimeMillis();
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "start getPDRI at:{0}", startGetPDRI);
        pdri = getPDRI(fileUID);

        // URL-decode the file name (might contain spaces and on) and prepare file object.
//        File file = new File("", URLDecoder.decode(requestedFile, "UTF-8"));
        // Check if file actually exists in filesystem.
        if (pdri == null) {
            // Do your thing if the file appears to be non-existing.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare some variables. The ETag is an unique identifier of the file.
        long length = pdri.getLength();
        LogicalDataWrapped ldw = logicalDataCache.get(fileUID);
        long lastModified;
        if (ldw != null) {
            LogicalData ld = ldw.getLogicalData();
            lastModified = ld.getModifiedDate();

        } else {
            lastModified = System.currentTimeMillis();
        }
        String eTag = fileLogicalName + "_" + length + "_" + lastModified;
        long expires = System.currentTimeMillis() + DEFAULT_EXPIRE_TIME;

        // Validate request headers for caching ---------------------------------------------------
        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", eTag); // Required in 304.
            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", eTag); // Required in 304.
            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }

        // Validate request headers for resume ----------------------------------------------------
        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // Validate and process range -------------------------------------------------------------
        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

        // Prepare and initialize response --------------------------------------------------------
        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = getServletContext().getMimeType(fileLogicalName);
        ldw = logicalDataCache.get(fileUID);
        if (contentType == null && ldw != null) {
            contentType = ldw.getLogicalData().getContentTypesAsString();
        } else {
            contentType = "application/octet-stream";
        }
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the browser and expand content type with the one and right character encoding.
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        } // Else, expect for images, determine content disposition. If content type is supported by
        // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
        else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }

        // Initialize response.
        response.reset();
        if (setResponseBufferSize) {
            response.setBufferSize(bufferSize);
        }
        responseBufferSize = response.getBufferSize();
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileLogicalName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);
        ldw = logicalDataCache.get(fileUID);
        if (ldw != null) {
            response.setContentLength(safeLongToInt(ldw.getLogicalData().getLength()));
        }


        // Send requested file (part(s)) to client ------------------------------------------------
        // Prepare streams.
//        RandomAccessFile input = null;
        OutputStream output = null;

        try {
            // Open streams.
//            input = new RandomAccessFile(file, "r");
//            input = pdri.getData()
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                Range r = full;
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);

                if (content) {
                    if (acceptsGzip) {
                        // The browser accepts GZIP, so GZIP the content.
                        response.setHeader("Content-Encoding", "gzip");
                        output = new GZIPOutputStream(output, bufferSize);
                    } else {
                        // Content length is not directly predictable in case of GZIP.
                        // So only add it if there is no means of GZIP, else browser will hang.
                        response.setHeader("Content-Length", String.valueOf(r.length));
                    }

                    // Copy full range.
                    copy(pdri, output, r.start, r.length, request);
                }

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Copy single part range.
                    copy(pdri, output, r.start, r.length, request);
                }

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                if (content) {
                    // Cast back to ServletOutputStream to get the easy println methods.
                    ServletOutputStream sos = (ServletOutputStream) output;

                    // Copy multi part range.
                    for (Range r : ranges) {
                        // Add multipart boundary and header fields for every range.
                        sos.println();
                        sos.println("--" + MULTIPART_BOUNDARY);
                        sos.println("Content-Type: " + contentType);
                        sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                        // Copy single part range of multi part range.
                        copy(pdri, output, r.start, r.length, request);
                    }

                    // End with multipart boundary.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY + "--");
                }
            }
        } finally {
            // Gently close streams.
            close(output);
            if (in != null) {
                in.close();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed <= 0) {
                elapsed = 1;
            }
            double speed = ((this.logicalDataCache.get(fileUID).getLogicalData().getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
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
            Stats stats = new Stats();
            stats.setSource(request.getLocalAddr());
            stats.setDestination(request.getRemoteAddr());
            stats.setSpeed(speed);
            stats.setSize(this.logicalDataCache.get(fileUID).getLogicalData().getLength());
            setSpeed(stats);

            String speedMsg = "Source: " + request.getLocalAddr() + " Destination: " + request.getRemoteAddr() + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + this.logicalDataCache.get(fileUID).getLogicalData().getLength() + " bytes";
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, speedMsg);
            String averageSpeedMsg = "Average speed: Source: " + pdri.getHost() + " Destination: " + request.getLocalAddr() + " Rx_Speed: " + averagre + " Kbites/sec Rx_Size: " + this.logicalDataCache.get(fileUID).getLogicalData().getLength() + " bytes";
            if (Util.sendStats()) {
                stats.setSource(request.getLocalAddr());
                stats.setDestination(request.getRemoteAddr());
                stats.setSpeed(speed);
                stats.setSize(this.logicalDataCache.get(fileUID).getLogicalData().getLength());
                setSpeed(stats);
            }
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, averageSpeedMsg);
        }
    }

    // Helpers (can be refactored to public utility class) ----------------------------------------
    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader The match header.
     * @param toMatch The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index
     * to the given end index as a long. If the substring is empty, then -1 will
     * be returned
     *
     * @param value The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as
     * long.
     * @param endIndex The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring
     * is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input
     * for.
     * @param start Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */
    private void copy(PDRI input, OutputStream output, long start, long length, HttpServletRequest request)
            throws IOException, VlException, JAXBException, URISyntaxException {
        OutputStream tos = null;
        try {
            if (input.getLength() == length) {
                // Write full range.
                in = input.getData();
                byte[] buffer = new byte[bufferSize];
                if (!isPDRIOnWorker(new URI(input.getURI()))) {
                    cacheFile = new File(cacheDir, input.getFileName());
                    if (!cacheFile.exists() || input.getLength() != cacheFile.length()) {
                        cacheDir = new File(System.getProperty("java.io.tmpdir") + File.separator + Util.getBackendWorkingFolderName());
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs();
                        }
                        tos = new TeeOutputStream(new FileOutputStream(cacheFile), output);
                        File file = new File(System.getProperty("user.home"));
                        while (file.getUsableSpace() < Util.getCacheFreeSpaceLimit()) {
                            evictCache();
                        }
                    }
                }
                if (tos == null) {
                    tos = output;
                }
                if (!qosCopy) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        tos.write(buffer, 0, len);
                    }

//                    CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((bufferSize), in, tos);
//                    cBuff.setMaxReadChunkSize(bufferSize);
//                    if (responseBufferSize > 0) {
//                        cBuff.setMaxWriteChunkSize(responseBufferSize);
//                    }
//                    cBuff.startTransfer(new Long(-1));
                } else {
                    qoSCopy(buffer, tos, length, request);
                }
            } else {
                io.milton.http.Range range = new io.milton.http.Range(start, length - start);
                input.copyRange(range, output);
//                input.copyRange(output, start, length);
            }
        } finally {
            if (tos != null) {
                tos.flush();
            } else {
                output.flush();
            }
        }
    }

    private void qoSCopy(byte[] buffer, OutputStream output, long size, HttpServletRequest request) throws IOException, JAXBException {
        int read;
        long total = 0;
        double speed;
        long startTime = System.currentTimeMillis();
        int count = 0;
        String d = "";
        double maxSpeed = -1;
        double thresshold = 100.0 * Math.exp(coefficient * (size / (1024.0 * 1024.0)));
        double averageSpeed = -1;
        double averageSpeedPrev = -2;
        while ((read = in.read(buffer)) > 0) {
            output.write(buffer, 0, read);
            total += read;
            double progress = (100.0 * total) / size;
            if (progress >= thresshold && Math.round(progress) % progressThresshold == 0 && !Util.dropConnection() && !Util.getOptimizeFlow()) {
                long elapsed = System.currentTimeMillis() - startTime;
                double a = 0.5;
                if (elapsed < 1) {
                    speed = Double.MAX_VALUE;
                } else {
                    speed = (total / elapsed);
                }
                if (averageSpeed <= 0) {
                    averageSpeed = speed;
                }
                averageSpeed = a * averageSpeed + (1 - a) * speed;
                if (averageSpeed >= maxSpeed) {
                    maxSpeed = averageSpeed;
                }
                d += "progressThresshold: " + progressThresshold + " speed: " + speed + " averageSpeed: " + averageSpeed + " progress: " + progress + " maxSpeed: " + maxSpeed + " limit: " + (maxSpeed / Util.getRateOfChangeLim()) + "\n";
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, d);
                if (averageSpeed < (maxSpeed / Util.getRateOfChangeLim()) && averageSpeed < averageSpeedPrev) {
                    count++;
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.WARNING, "We will not tolarate this !!!! Next time line is off");
                    if (Util.getOptimizeFlow()) {
                        optimizeFlow(request);
                        maxSpeed = averageSpeed;
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "optimizeFlow: {0}", request);
                    }

                    //This works with export ec=18; while [ $ec -eq 18 ]; do curl -O -C - -L --request GET -u user:pass http://localhost:8080/lobcder/dav/large_file; export ec=$?; done
                    if (count >= warnings && Util.dropConnection()) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.WARNING, "We will not tolarate this !!!! Find a new worker. rateOfChange: {0}", averageSpeed);
                        break;
                    }
                }
                averageSpeedPrev = averageSpeed;
            }
        }
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, d);
    }

    /**
     * Close the given resource.
     *
     * @param resource The resource to be closed.
     */
    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
                // Ignore IOException. If you want to handle this anyway, it might be useful to know
                // that this will generally only be thrown when the client aborted the request.
            }
        }
    }

    private ClientConfig configureClient() {
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

    private PDRI getPDRI(String fileUID) throws InterruptedException, IOException, URISyntaxException {
        PDRIDescr pdriDesc = null;//new PDRIDescr();
        LogicalDataWrapped logicalData = null;
        logicalData = logicalDataCache.get(fileUID);
        if (logicalData == null) {
            if (restClient == null) {
                restClient = Client.create(clientConfig);
                restClient.removeAllFilters();
                restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
                webResource = restClient.resource(restURL);
            }

            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Asking master. Token: {0}", token);
            WebResource res = webResource.path("item").path("query").path(fileUID);
            logicalData = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<LogicalDataWrapped>() {
            });
            logicalData = removeUnreachablePDRIs(logicalData);
        }

        logicalData = addCacheFileToPDRIDescr(logicalData);


        pdriDesc = selectBestPDRI(logicalData.getPdriList());

        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selected pdri: {0}", pdriDesc.getResourceUrl());
        fileLogicalName = logicalData.getLogicalData().getName();
        return new VPDRI(pdriDesc.getName(), pdriDesc.getId(), pdriDesc.getResourceUrl(), pdriDesc.getUsername(),
                pdriDesc.getPassword(), pdriDesc.getEncrypt(), pdriDesc.getKey(), false);
    }

    private PDRIDescr selectBestPDRI(List<PDRIDescr> pdris) throws URISyntaxException, UnknownHostException, SocketException {
        if (!pdris.isEmpty()) {
            Iterator<PDRIDescr> iter = pdris.iterator();
            while (iter.hasNext()) {
                PDRIDescr p = iter.next();
                URI uri = new URI(p.getResourceUrl());
                if (uri.getScheme().equals("file")) {
                    return p;
                }
                if (isPDRIOnWorker(uri)) {
                    String resURL = p.getResourceUrl().replaceFirst(uri.getScheme(), "file");
                    p.setResourceUrl(resURL);
                    return p;
                }
            }

        }
        if (weightPDRIMap.isEmpty() || weightPDRIMap.size() < pdris.size()) {
            //Just return one at random;
            int index = new Random().nextInt(pdris.size());
            PDRIDescr[] array = pdris.toArray(new PDRIDescr[pdris.size()]);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selecting Random: {0}", array[index].getResourceUrl());
            return array[index];
        }

        long sumOfSpeed = 0;
        for (PDRIDescr p : pdris) {
            URI uri = new URI(p.getResourceUrl());
            String host = null;
            if (uri.getScheme().equals("file")
                    || StringUtil.isEmpty(uri.getHost())
                    || uri.getHost().equals("localhost")
                    || uri.getHost().equals("127.0.0.1")) {
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } catch (Exception ex) {
                    List<String> ips = Util.getAllIPs();
                    for (String ip : ips) {
                        if (ip.contains(".")) {
                            host = ip;
                            break;
                        }
                    }
                }
            } else {
                host = uri.getHost();
            }
            Double speed = weightPDRIMap.get(host);
            if (speed == null) {
                speed = Double.valueOf(0);
            }
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Speed: {0}", speed);
            sumOfSpeed += speed;
        }
        if (sumOfSpeed <= 0) {
            int index = new Random().nextInt(pdris.size());
            PDRIDescr[] array = pdris.toArray(new PDRIDescr[pdris.size()]);
            return array[index];
        }
        int itemIndex = new Random().nextInt((int) sumOfSpeed);

        for (PDRIDescr p : pdris) {
            Double speed = weightPDRIMap.get(new URI(p.getResourceUrl()).getHost());
            if (speed == null) {
                speed = Double.valueOf(0);
            }
            if (itemIndex < speed) {
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selecting:{0}  with speed: {1}", new Object[]{p.getResourceUrl(), speed});
                return p;
            }
            itemIndex -= speed;
        }

        int index = new Random().nextInt(pdris.size());
        PDRIDescr[] array = pdris.toArray(new PDRIDescr[pdris.size()]);
        PDRIDescr res = array[index];
        return res;
    }

    private void handleError(java.lang.Exception ex, HttpServletResponse response) throws IOException {
        if (ex instanceof IOException) {
            if (ex.getMessage().contains("PDRIS from master is either empty or contains unreachable files")) {
                response.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY);
                return;
            }
            if (ex.getMessage().contains("Low B/W")) {
//                                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
        }
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
    }

    private void setSpeed(Stats stats) throws JAXBException {
        if (sendStats) {
            JAXBContext context = JAXBContext.newInstance(Stats.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            OutputStream out = new ByteArrayOutputStream();
            m.marshal(stats, out);

            String stringStats = String.valueOf(out);

            if (restClient == null) {
                restClient = Client.create(clientConfig);
                restClient.removeAllFilters();
                restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
                webResource = restClient.resource(restURL);
            }

            ClientResponse response = webResource.path("lob_statistics").path("set")
                    .type(MediaType.APPLICATION_XML).put(ClientResponse.class, stringStats);
        }
    }

    private void optimizeFlow(HttpServletRequest request) throws JAXBException {
        Endpoints endpoints = new Endpoints();
        endpoints.setDestination(request.getRemoteAddr());
        endpoints.setSource(request.getLocalAddr());

        JAXBContext context = JAXBContext.newInstance(Endpoints.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        OutputStream out = new ByteArrayOutputStream();
        m.marshal(endpoints, out);

        WebResource webResource = restClient.resource(restURL);
        String stringStats = String.valueOf(out);

        ClientResponse response = webResource.path("sdn").path("optimizeFlow")
                .type(MediaType.APPLICATION_XML).put(ClientResponse.class, stringStats);

        Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "response: {0}", response);
    }

    private static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private void evictCache() throws IOException {
        String key = null;
        switch (Util.getCacheEvictionPolicy()) {
            case LRU:
            case LFU:
                Set<Map.Entry<String, Long>> set = fileAccessMap.entrySet();
                Long min = Long.MAX_VALUE;
                for (Map.Entry<String, Long> e : set) {
                    if (e.getValue() < min) {
                        min = e.getValue();
                        key = e.getKey();
                    }
                }
                break;
            case MRU:
            case MFU:
                set = fileAccessMap.entrySet();
                Long max = Long.MIN_VALUE;
                for (Map.Entry<String, Long> e : set) {
                    if (e.getValue() > max) {
                        max = e.getValue();
                        key = e.getKey();
                    }
                }
                break;
            case RR:
            default:
                Random random = new Random();
                List<String> keys = new ArrayList(fileAccessMap.keySet());
                key = keys.get(random.nextInt(keys.size()));
                break;

        }
        new File(key).delete();
        fileAccessMap.remove(key);
    }

    private LogicalDataWrapped addCacheFileToPDRIDescr(LogicalDataWrapped logicalData) throws IOException, URISyntaxException {
        List<PDRIDescr> pdris = logicalData.getPdriList();
        cacheFile = new File(cacheDir, pdris.get(0).getName());
        if (!isFileOnWorker(logicalData) && cacheFile.exists() && !isCacheInPdriList(cacheFile, logicalData)) {
            String fileName = cacheFile.getName();
            long ssID = -1;
            String resourceURI = "file:///" + cacheFile.getAbsoluteFile().getParentFile().getParent();
            String uName = "fake";
            String passwd = "fake";
            boolean encrypt = false;
            long key = -1;
            long pdriId = -1;
            Long groupId = Long.valueOf(-1);
            pdris.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), groupId, pdriId));
            switch (Util.getCacheEvictionPolicy()) {
                case LRU:
                case MRU:
                case RR:
                    fileAccessMap.put(cacheFile.getAbsolutePath(), System.currentTimeMillis());
                    break;
                case LFU:
                case MFU:
                    Long count = fileAccessMap.get(cacheFile.getAbsolutePath());
                    if (count == null) {
                        count = Long.valueOf(0);
                    }
                    fileAccessMap.put(cacheFile.getAbsolutePath(), count++);
                    break;
            }
            logicalData.setPdriList(pdris);
        }
        return logicalData;
    }

    private LogicalDataWrapped removeUnreachablePDRIs(LogicalDataWrapped logicalData) throws IOException, URISyntaxException {
        List<PDRIDescr> pdris = logicalData.getPdriList();
        if (pdris != null) {
            List<PDRIDescr> removeIt = new ArrayList<>();
            for (PDRIDescr p : pdris) {
                URI uri = new URI(p.getResourceUrl());
                String pdriHost = uri.getHost();
                String pdriScheme = uri.getScheme();
                if (pdriHost == null || pdriHost.equals("localhost") || pdriHost.startsWith("127.0.")) {
                    removeIt.add(p);
                } else if (pdriScheme.equals("file") && !isPDRIOnWorker(new URI(p.getResourceUrl()))) {
                    removeIt.add(p);
                }
            }
            if (!removeIt.isEmpty()) {
                pdris.removeAll(removeIt);
                if (pdris.isEmpty()) {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, "PDRIS from master is either empty or contains unreachable files");
                    logicalDataCache.remove(fileUID);
                    throw new IOException("PDRIS from master is either empty or contains unreachable files");
                }
                logicalDataCache.put(fileUID, logicalData);
            }
        }
        return logicalData;
    }

    private void authenticate(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String autheader = httpRequest.getHeader("Authorization");
        if (autheader != null) {

            final int index = autheader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index).getBytes()), "UTF8");
//                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);


                double start = System.currentTimeMillis();

                AuthTicket a = new AuthTicket();
                MyPrincipal principal = a.checkToken(uname, token);


                String method = ((HttpServletRequest) httpRequest).getMethod();
                StringBuffer reqURL = ((HttpServletRequest) httpRequest).getRequestURL();
                double elapsed = System.currentTimeMillis() - start;

                String userAgent = ((HttpServletRequest) httpRequest).getHeader("User-Agent");

                String from = ((HttpServletRequest) httpRequest).getRemoteAddr();
//        String user = ((HttpServletRequest) httpRequest).getRemoteUser();
                int contentLen = ((HttpServletRequest) httpRequest).getContentLength();
                String contentType = ((HttpServletRequest) httpRequest).getContentType();

                String authorizationHeader = ((HttpServletRequest) httpRequest).getHeader("authorization");
                String userNpasswd = "";
                if (authorizationHeader != null) {
                    userNpasswd = authorizationHeader.split("Basic ")[1];
                }
                String queryString = ((HttpServletRequest) httpRequest).getQueryString();

                if (principal != null) {
                    httpRequest.setAttribute("myprincipal", principal);
                    return;
                }
            }
        }
        String _realm = "SECRET";
        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + _realm + "\"");
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void getPDRIs() throws IOException, URISyntaxException {
        if (restClient == null) {
            restClient = Client.create(clientConfig);
            restClient.removeAllFilters();
            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
            webResource = restClient.resource(restURL);

            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.add("path", "/");
            WebResource res = webResource.path("items").path("query").queryParams(params);

            List<LogicalDataWrapped> logicalDataList = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<List<LogicalDataWrapped>>() {
            });
            for (LogicalDataWrapped ldw : logicalDataList) {
                if (!ldw.getLogicalData().isFolder()) {
                    LogicalDataWrapped ld = removeUnreachablePDRIs(ldw);
                    this.logicalDataCache.put(String.valueOf(ld.getLogicalData().getUid()), ld);
                }
            }
        }
    }

    private boolean isFileOnWorker(LogicalDataWrapped logicalData) throws URISyntaxException, UnknownHostException, SocketException {
        List<PDRIDescr> pdris = logicalData.getPdriList();
        for (PDRIDescr p : pdris) {
            if (isPDRIOnWorker(new URI(p.getResourceUrl()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPDRIOnWorker(URI pdriURI) throws URISyntaxException, UnknownHostException, SocketException {
        String pdriHost = pdriURI.getHost();
        if (pdriHost == null || pdriHost.equals("localhost") || pdriHost.startsWith("127.")) {
            return false;
        }
        List<String> workerIPs = Util.getAllIPs();
        String resourceIP = Util.getIP(pdriURI.getHost());
        for (String ip : workerIPs) {
            if (ip != null) {
                if (ip.equals(pdriHost) || ip.equals(resourceIP)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCacheInPdriList(File cacheFile, LogicalDataWrapped logicalData) {
        String resourceURI = "file:///" + cacheFile.getAbsoluteFile().getParentFile().getParent();
        List<PDRIDescr> pdris = logicalData.getPdriList();
        if (pdris != null) {
            for (PDRIDescr p : pdris) {
                if (p.getResourceUrl().equals(resourceURI)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This class represents a byte range.
     */
    protected class Range {

        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }
}

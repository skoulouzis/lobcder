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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import javax.xml.bind.annotation.XmlRootElement;
import lombok.extern.java.Log;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.vfs.VFSTransfer;
import nl.uva.vlet.vrl.VRL;

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
    private Boolean useCircularBuffer;
    private String restURL;
    private String token;
    private ClientConfig clientConfig;
    private String fileUID;
    private final Map<String, LogicalDataWrapped> logicalDataCache = new HashMap<>();
    private Client restClient;
    private long sleepTime = 2;
    private static final Map<String, Double> weightPDRIMap = new HashMap<>();
    private static final HashMap<String, Integer> numOfGetsMap = new HashMap<>();
    private InputStream in;
    private int responseBufferSize;
    private double lim = -5.0;
    private boolean qosCopy;
    private int warnings;
    private double progressThresshold;
    private double coefficient;

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
            useCircularBuffer = Util.isCircularBuffer();
            qosCopy = Util.doQosCopy();
            bufferSize = Util.getBufferSize();

            restURL = Util.getRestURL();
            token = Util.getRestPassword();
            lim = Util.getRateOfChangeLim();
            //        uname = prop.getProperty(("rest.uname"));
            clientConfig = configureClient();
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            warnings = Util.getNumOfWarnings();
            progressThresshold = Util.getProgressThresshold();
            coefficient = Util.getProgressThressholdCoefficient();
        } catch (IOException ex) {
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
            throws ServletException, IOException {
        try {
            // Process request with content.
            processRequest(request, response, true);
        } catch (Exception ex) {
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
            handleError(ex, response);
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
            throws IOException, InterruptedException, URISyntaxException, VlException {
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
        fileUID = pathAndToken.getParent().toString();
//            cacheFile = new File(baseDir, "lobcder-cache" + request.getLocalName());
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "token: {0} fileUID: {1}", new Object[]{token, fileUID});


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
        String fileName = pdri.getFileName();
        long length = pdri.getLength();
        long lastModified = logicalDataCache.get(fileUID).logicalData.modifiedDate;
        String eTag = fileName + "_" + length + "_" + lastModified;
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
        String contentType = getServletContext().getMimeType(fileName);
        if (contentType == null) {
            contentType = this.logicalDataCache.get(fileUID).logicalData.contentTypesAsString;
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
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);
        response.setContentLength((int) this.logicalDataCache.get(fileUID).logicalData.length);

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
                    copy(pdri, output, r.start, r.length);
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
                    copy(pdri, output, r.start, r.length);
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
                        copy(pdri, output, r.start, r.length);
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
            double speed = ((this.logicalDataCache.get(fileUID).logicalData.length * 8.0) * 1000.0) / (elapsed * 1000.0);
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

            String speedMsg = "Source: " + request.getLocalAddr() + " Destination: " + request.getRemoteAddr() + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + this.logicalDataCache.get(fileUID).logicalData.length + " bytes";
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, speedMsg);
            String averageSpeedMsg = "Average speed: Source: " + pdri.getHost() + " Destination: " + request.getLocalAddr() + " Rx_Speed: " + averagre + " Kbites/sec Rx_Size: " + this.logicalDataCache.get(fileUID).logicalData.length + " bytes";
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
    private void copy(PDRI input, OutputStream output, long start, long length)
            throws IOException, VlException {
        try {
            if (input.getLength() == length) {
                // Write full range.
                in = input.getData();
                byte[] buffer = new byte[bufferSize];
                if (!qosCopy) {
                    CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((bufferSize), in, output);
                    cBuff.setMaxReadChunkSize(bufferSize);
                    if (responseBufferSize > 0) {
                        cBuff.setMaxWriteChunkSize(responseBufferSize);
                    }
                    cBuff.startTransfer(new Long(-1));
                } else {
                    qoSCopy(buffer, output, length);
                }
            } else {
                input.copyRange(output, start, length);
            }
        } finally {
            output.flush();
        }
    }

    private void qoSCopy(byte[] buffer, OutputStream output, long size) throws IOException {
        int read;
        long total = 0;
        double speed;
        double rateOfChange = 0;
        double speedPrev = 0;
        long startTime = System.currentTimeMillis();
        int count = 0;
        String d = "";
        double maxSpeed = -1;
        double thresshold = 100.0 * Math.exp(coefficient * (size / (1024.0 * 1024.0)));
        while ((read = in.read(buffer)) > 0) {
            output.write(buffer, 0, read);
            total += read;
            double progress = (100.0 * total) / size;
            if (progress >= thresshold && Math.round(progress) % progressThresshold == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                speed = (total / elapsed);
                if (speed >= maxSpeed) {
                    maxSpeed = speed;
                }
                rateOfChange = (100.0 * maxSpeed) / speedPrev;
                speedPrev = speed;
                d += "progressThresshold: " + thresshold + " speed: " + speed + " rateOfChange: " + rateOfChange + " speedPrev: " + speedPrev + " progress: " + progress + "\n";
                if (rateOfChange < lim) {
                    count++;
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.WARNING, "We will not tolarate this !!!! Next time line is off");
                    //This works with export ec=18; while [ $ec -eq 18 ]; do curl -O -C - -L --request GET -u user:pass http://localhost:8080/lobcder/dav/large_file; export ec=$?; done
                    if (count >= warnings) {
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.WARNING, "We will not tolarate this !!!! Find a new worker. rateOfChange: {0}", rateOfChange);
                        break;
                    }
                }
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
        PDRIDesc pdriDesc = null;//new PDRIDesc();
        LogicalDataWrapped logicalData = null;

        logicalData = logicalDataCache.get(fileUID);
        if (logicalData == null) {
            if (restClient == null) {
                restClient = Client.create(clientConfig);
            }
            restClient.removeAllFilters();
            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
            WebResource webResource = restClient.resource(restURL);
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Asking master. Token: {0}", token);
            WebResource res = webResource.path("item").path("query").path(fileUID);
            logicalData = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<LogicalDataWrapped>() {
            });

            if (logicalData != null) {
                Set<PDRIDesc> pdris = logicalData.pdriList;
                List<PDRIDesc> removeIt = new ArrayList<>();
                if (pdris != null && !pdris.isEmpty()) {
                    //Remove masters's cache pdris 
                    for (PDRIDesc p : pdris) {
                        if (p.resourceUrl.startsWith("file")) {
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
                    }
                    logicalData.pdriList = pdris;
                }
            }
            logicalDataCache.put(fileUID, logicalData);
        }


        pdriDesc = selectBestPDRI(logicalData.pdriList);

        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selected pdri: {0}", pdriDesc.resourceUrl);
        return new VPDRI(pdriDesc.name, pdriDesc.id, pdriDesc.resourceUrl, pdriDesc.username, pdriDesc.password, pdriDesc.encrypt, BigInteger.valueOf(Long.valueOf(pdriDesc.key)), false);
    }

    private PDRIDesc selectBestPDRI(Set<PDRIDesc> pdris) throws URISyntaxException, UnknownHostException, SocketException {
        if (!pdris.isEmpty()) {
            PDRIDesc p = pdris.iterator().next();
            URI uri = new URI(p.resourceUrl);
            String resourceIP = Util.getIP(uri.getHost());
            List<String> ips = Util.getAllIPs();
            for (String i : ips) {
//                Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "resourceIP: {0} localIP: {1}", new Object[]{resourceIP, i});
                if (resourceIP != null && resourceIP.equals(i)
                        || uri.getHost().equals("localhost")
                        || uri.getHost().equals("127.0.0.1")) {
                    String resURL = p.resourceUrl.replaceFirst(uri.getScheme(), "file");
                    p.resourceUrl = resURL;
                    return p;
                }
            }

        }

        for (PDRIDesc p : pdris) {
            URI uri = new URI(p.resourceUrl);
            if (uri.getScheme().equals("file")) {
                return p;
            }
            String resourceIP = Util.getIP(uri.getHost());
            List<String> ips = Util.getAllIPs();
            for (String i : ips) {
//                Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "Checking IP: {0}", i);
                if (resourceIP.equals(i)) {
                    String resURL = p.resourceUrl.replaceFirst(uri.getScheme(), "file");
                    p.resourceUrl = resURL;
                    return p;
                }
            }

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
        PDRIDesc res = array[index];
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

    // Inner classes ------------------------------------------------------------------------------
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

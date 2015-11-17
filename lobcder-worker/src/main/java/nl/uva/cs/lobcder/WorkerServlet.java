/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import io.milton.common.Path;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import lombok.extern.java.Log;
import static nl.uva.cs.lobcder.Util.ChacheEvictionAlgorithm.LRU;
import static nl.uva.cs.lobcder.Util.ChacheEvictionAlgorithm.MRU;
import nl.uva.cs.lobcder.auth.AuthTicket;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.rest.wrappers.LogicalDataWrapped;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.vlet.exception.VlException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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
    private String fileUID;
    private static final HashMap<String, Integer> numOfGetsMap = new HashMap<>();
    private InputStream in;
    private boolean qosCopy;
    private int warnings;
    private double progressThresshold;
    private double coefficient;
    private String fileLogicalName;
    private File cacheFile;
    private Catalogue cat;

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


            warnings = Util.getNumOfWarnings();
            progressThresshold = Util.getProgressThresshold();
            coefficient = Util.getProgressThressholdCoefficient();


            this.cat = new Catalogue();
            TimerTask gcTask = new SweepersTimerTask(cat);
            Timer timer = new Timer(true);
            timer.schedule(gcTask, 1000, 1000);

            cat.getPDRIs(fileUID);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            //        curl -viks  --post302 -L -u user:pass -X POST -F key1=value1 -F upload=@file http://localhost:8080/lobcder/dav/

            // checks if the request actually contains upload file
            if (!ServletFileUpload.isMultipartContent(request)) {
                // if not, we stop here
                PrintWriter writer = response.getWriter();
                writer.println("Error: Form must has enctype=multipart/form-data.");
                writer.flush();
                return;
            }

            // configures upload settings
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // sets memory threshold - beyond which files are stored in disk
            factory.setSizeThreshold(this.bufferSize);
            // sets temporary location to store files
            factory.setRepository(Util.getUploadDir());

            ServletFileUpload upload = new ServletFileUpload(factory);
            // sets maximum size of upload file
            //        upload.setFileSizeMax(MAX_FILE_SIZE);
            // sets maximum size of request (include file + form data)
            //        upload.setSizeMax(MAX_REQUEST_SIZE);

            Map<String, Triple<Long, Long, Collection<Long>>> storagMap = parseQuery(request.getQueryString());

            List<FileItem> formItems = upload.parseRequest(request);
            Iterator<FileItem> iter = formItems.iterator();
            FileItem item;
//
            while (iter.hasNext()) {
                item = iter.next();
                if (item.getName() == null) {
                    continue;
                }
                String fileName = item.getName();
                Triple<Long, Long, Collection<Long>> triple = storagMap.get(fileName);
                StringBuilder storeName = new StringBuilder();
                storeName.append(triple.getLeft()).append("-");
                storeName.append(triple.getMiddle()).append("-");
                for (Long l : triple.getRight()) {
                    storeName.append(l).append("-");
                }
                storeName.deleteCharAt(storeName.length() - 1);
                String filePath = Util.getUploadDir() + File.separator + fileName + "_" + storeName.toString();
                File storeFile = new File(filePath);
                item.write(storeFile);
            }
        } catch (Exception ex) {
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
            String fname = this.cacheFile.getAbsolutePath();
            Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, "cacheFile: " + cacheFile.getAbsolutePath() + " fileLogicalName: " + fileLogicalName + " fileUID: " + this.fileUID, ex);
            this.cat.logicalDataCache.remove(fileUID);
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

        pdri = cat.getPDRI(fileUID);
        fileLogicalName = cat.getLogicalDataWrapped(fileUID).getLogicalData().getName();
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
        LogicalDataWrapped ldw = Catalogue.logicalDataCache.get(fileUID);
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
        ldw = Catalogue.logicalDataCache.get(fileUID);
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
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileLogicalName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);
        ldw = Catalogue.logicalDataCache.get(fileUID);
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
            LogicalDataWrapped lwd = Catalogue.logicalDataCache.get(fileUID);
            if (lwd != null) {
                double speed = ((lwd.getLogicalData().getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
                Double oldSpeed = Catalogue.weightPDRIMap.get(pdri.getHost());
                if (oldSpeed == null) {
                    oldSpeed = speed;
                }
                Integer numOfGets = numOfGetsMap.get(pdri.getHost());
                if (numOfGets == null) {
                    numOfGets = 1;
                }
                double averagre = (speed + oldSpeed) / (double) numOfGets;
                numOfGetsMap.put(pdri.getHost(), numOfGets++);
                Catalogue.weightPDRIMap.put(pdri.getHost(), averagre);
                Stats stats = new Stats();
                stats.setSource(request.getLocalAddr());
                stats.setDestination(request.getRemoteAddr());
                stats.setSpeed(speed);
                stats.setSize(Catalogue.logicalDataCache.get(fileUID).getLogicalData().getLength());
                cat.setSpeed(stats);
                String speedMsg = "Source: " + request.getLocalAddr() + " Destination: " + request.getRemoteAddr() + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + Catalogue.logicalDataCache.get(fileUID).getLogicalData().getLength() + " bytes";
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, speedMsg);
                String averageSpeedMsg = "Average speed: Source: " + pdri.getHost() + " Destination: " + request.getLocalAddr() + " Rx_Speed: " + averagre + " Kbites/sec Rx_Size: " + Catalogue.logicalDataCache.get(fileUID).getLogicalData().getLength() + " bytes";
                if (Util.sendStats()) {
                    stats.setSource(request.getLocalAddr());
                    stats.setDestination(request.getRemoteAddr());
                    stats.setSpeed(speed);
                    stats.setSize(Catalogue.logicalDataCache.get(fileUID).getLogicalData().getLength());
                    cat.setSpeed(stats);
                }
                Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, averageSpeedMsg);
            }
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
                if (!Catalogue.isPDRIOnWorker(new URI(input.getURI()))) {
                    cacheFile = new File(Util.getCacheDir(), input.getFileName());
                    if (!cacheFile.exists() || input.getLength() != cacheFile.length()) {
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
                        cat.optimizeFlow(request);
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
                Set<Map.Entry<String, Long>> set = Catalogue.fileAccessMap.entrySet();
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
                set = Catalogue.fileAccessMap.entrySet();
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
                List<String> keys = new ArrayList(Catalogue.fileAccessMap.keySet());
                key = keys.get(random.nextInt(keys.size()));
                break;

        }
        File deleteIt = null;
        if (key != null) {
            deleteIt = new File(key);
        }
        if (deleteIt != null) {
            deleteIt.delete();
        }
        Catalogue.fileAccessMap.remove(key);
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

    Map<String, Triple<Long, Long, Collection<Long>>> parseQuery(String query) throws UnsupportedEncodingException {
//        file_id=9&ss_id=8&file_id=3&ss_id=8
        Map<String, org.apache.commons.lang3.tuple.Triple<Long, Long, Collection<Long>>> storageMap = new HashMap<>();
        MutableTriple<Long, Long, Collection<Long>> triple;
        final String[] files = query.split("&");
        for (String file : files) {
            if (file != null && file.length() > 0) {
                String[] parts = file.split("/");
                triple = new MutableTriple<>();
                String fileName = null;
                Long fileUid = null;
                Long pdrigroupUid = null;
                String ssid;
                Collection<Long> ssids = new ArrayList<>();
                for (String part : parts) {
                    if (part.startsWith("file_name=")) {
                        fileName = part.split("=")[1];
                    } else if (part.startsWith("file_uid=")) {
                        fileUid = Long.valueOf(part.split("=")[1]);
                    } else if (part.startsWith("pdrigroup_uid=")) {
                        pdrigroupUid = Long.valueOf(part.split("=")[1]);
                    } else if (part.startsWith("ss_id=")) {
                        ssid = part.split("=")[1];
                        ssids.add(Long.valueOf(ssid));
                    }
                }
                triple.setLeft(fileUid);
                triple.setMiddle(pdrigroupUid);
                triple.setRight(ssids);
                storageMap.put(fileName, triple);
            }
        }
        return storageMap;
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

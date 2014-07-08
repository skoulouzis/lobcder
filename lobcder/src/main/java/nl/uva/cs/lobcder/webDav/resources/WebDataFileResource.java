/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.ContentTypeUtils;
import io.milton.common.Path;
import io.milton.http.Auth;
import io.milton.http.FileItem;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.BufferingControlResource;
import io.milton.resource.CollectionResource;
import io.milton.resource.FileResource;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.AuthWorker;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.optimization.SDNControllerClient;
import nl.uva.cs.lobcder.optimization.SDNControllerClient.FloodlightStats;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataFileResource extends WebDataResource implements
        FileResource, BufferingControlResource {

    private int sleepTime = 5;
    private List<String> workers;
    private HashMap<String, String> workersMap;
    private boolean doRedirect = true;
    private static int workerIndex = 0;
    private static final Map<String, Double> weightPDRIMap = new HashMap<>();
    private static final Map<String, Integer> numOfGetsMap = new HashMap<>();
    private static final Map<String, Integer> numOfWorkerTransfersMap = new HashMap<>();
    private SDNControllerClient sdnClient;

    public WebDataFileResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        super(logicalData, path, catalogue, authList);
        try {
            doRedirect = PropertiesHelper.doRedirectGets();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (doRedirect) {
            List<String> workersURLs = PropertiesHelper.getWorkers();
            if (workersURLs == null || workersURLs.isEmpty()) {
                doRedirect = false;
            } else {
                workersMap = new HashMap<>();
                for (String s : workersURLs) {
                    try {
                        s = s.replaceAll(" ", "");
                        workersMap.put(new URI(s).getHost(), s);
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }


    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            return false;
        }
        switch (method) {
            case MKCOL:
                String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
                WebDataFileResource.log.log(Level.INFO, msg);
                return false;
            default:
                return super.authorise(request, method, auth);
        }
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        log.log(Level.FINE, "copyTo(''{0}'', ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(newParentPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().copyFile(getLogicalData(), toWDDR.getLogicalData(), name, getPrincipal(), connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public void moveTo(CollectionResource collectionResource, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource toWDDR = (WebDataDirResource) collectionResource;
        log.log(Level.FINE, "moveTo(''{0}'', ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions destPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                LogicalData parentLD = getCatalogue().getLogicalDataByUid(getLogicalData().getParentRef());
                Permissions parentPerm = getCatalogue().getPermissions(parentLD.getUid(), parentLD.getOwner());
                if (!(getPrincipal().canWrite(destPerm) && getPrincipal().canWrite(parentPerm))) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().moveEntry(getLogicalData(), toWDDR.getLogicalData(), name, connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, BadRequestException, ConflictException {
        log.log(Level.FINE, "delete() file {0}", getPath());
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                getCatalogue().remove(getLogicalData(), getPrincipal(), connection);
                connection.commit();
            } catch (Exception e) {
                log.log(Level.SEVERE, null, e);
                connection.rollback();
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this, e.getMessage());
        }
    }

    @Override
    public Long getContentLength() {
        log.log(Level.FINE, "getContentLength()" + " for {0}", getPath());
        return getLogicalData().getLength();
    }

    @Override
    public String getContentType(String accepts) {
        String res = ContentTypeUtils.findAcceptableContentType(getLogicalData().getContentTypesAsString(), accepts);
        log.log(Level.FINE, "getContentType(''accepts: {0}'') = ''{1}''  for {2}", new Object[]{accepts, res, getPath()});
        return res;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        log.log(Level.FINE, "getMaxAgeSeconds() for {0}", getPath());
        return null;
    }

    private PDRIDescr selectBestPDRI(List<PDRIDescr> pdris) throws URISyntaxException, UnknownHostException {
        if (pdris.size() == 1) {
            return pdris.iterator().next();
        }
        if (weightPDRIMap.isEmpty() || weightPDRIMap.size() < pdris.size()) {
            //Just return one at random;
            int index = new Random().nextInt(pdris.size());
            PDRIDescr[] array = pdris.toArray(new PDRIDescr[pdris.size()]);
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.FINE, "Selecting Random: {0}", array[index].getResourceUrl());
            return array[index];
        }

        long sumOfSpeed = 0;
        for (PDRIDescr p : pdris) {
            URI uri = new URI(p.getResourceUrl());
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
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.FINE, "Speed: : {0}", speed);
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
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.FINE, "Selecting:{0}  with speed: {1}", new Object[]{p.getResourceUrl(), speed});
                return p;
            }
            itemIndex -= speed;
        }

        int index = new Random().nextInt(pdris.size());
        PDRIDescr[] array = pdris.toArray(new PDRIDescr[pdris.size()]);
        return array[index];
    }

    private PDRI transferer(List<PDRIDescr> pdris, OutputStream out, int tryCount, boolean doCircularStreamBufferTransferer) throws IOException, NotFoundException {
        InputStream in = null;
        PDRI pdri = null;
        try {
            PDRIDescr pdriDescr = selectBestPDRI(pdris);
            pdri = PDRIFactory.getFactory().createInstance(pdriDescr, false);
            if (pdri != null) {
                in = pdri.getData();
                if (!pdri.getEncrypted()) {
                    if (doCircularStreamBufferTransferer) {
                        CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
                        cBuff.startTransfer((long) -1);
                    } else {
                        int read;
                        byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                            out.write(copyBuffer, 0, read);
                        }
                    }
                } else {
                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
                    encrypter.decrypt(in, out);
                }
            } else {
                sleepTime = 5;
                throw new NotFoundException("Physical resource not found");
            }
        } catch (Exception ex) {
            if (ex instanceof NotFoundException) {
                throw (NotFoundException) ex;
            }
            if (ex.getMessage().contains("Resource not found")) {
                throw new NotFoundException(ex.getMessage());
            }
            try {
                sleepTime = sleepTime + 20;
                Thread.sleep(sleepTime);
                if (ex instanceof nl.uva.vlet.exception.VlInterruptedException && ++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(pdris, out, tryCount, false);
                } else if (++tryCount < Constants.RECONNECT_NTRY) {
                    transferer(pdris, out, tryCount, true);
                } else {
                    transferer(pdris, out, 0, true);
                }
            } catch (InterruptedException ex1) {
                sleepTime = 5;
                throw new IOException(ex1);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        sleepTime = 5;
        return pdri;
    }

//    private PDRI transferer(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri, boolean doCircularStreamBufferTransferer) throws IOException, NotFoundException {
//        InputStream in;
//        try {
//            boolean reconnect;
//            if (pdri == null && it.hasNext()) {
//                pdri = PDRIFactory.getFactory().createInstance(it.next(), false);
//                reconnect = false;
//            } else {
//                reconnect = true;
//            }
//            if (pdri != null) {
//                if (reconnect) {
//                    pdri.reconnect();
//                }
//                in = pdri.getData();
//                WebDataFileResource.log.log(Level.FINE, "sendContent() for {0}--------- {1}", new Object[]{getPath(), pdri.getFileName()});
//                if (!pdri.getEncrypted()) {
//                    if (doCircularStreamBufferTransferer) {
//                        CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
//                        cBuff.startTransfer((long) -1);
//                    } else {
//                        int read;
//                        byte[] copyBuffer = new byte[Constants.BUF_SIZE];
//                        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
//                            out.write(copyBuffer, 0, read);
//                        }
//                    }
//                } else {
//                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
//                    encrypter.decrypt(in, out);
//                }
//            } else {
//                sleepTime = 5;
//                throw new NotFoundException("Physical resource not found");
//            }
//        } catch (Exception ex) {
//            if (ex instanceof NotFoundException) {
//                throw (NotFoundException) ex;
//            }
//            try {
//                sleepTime = sleepTime + 20;
//                Thread.sleep(sleepTime);
//                if (ex instanceof nl.uva.vlet.exception.VlInterruptedException && ++tryCount < Constants.RECONNECT_NTRY) {
//                    transferer(it, out, tryCount, pdri, false);
//                } else if (++tryCount < Constants.RECONNECT_NTRY) {
//                    transferer(it, out, tryCount, pdri, false);
//                } else {
//                    transferer(it, out, 0, null, true);
//                }
//            } catch (InterruptedException ex1) {
//                sleepTime = 5;
//                throw new IOException(ex1);
//            }
//        }
//        sleepTime = 5;
//        return pdri;
//    }
    private PDRI transfererRange(Iterator<PDRIDescr> it, OutputStream out, int tryCount, PDRI pdri, Range range) throws IOException, NotFoundException {
        try {
            boolean reconnect;
            if (pdri == null && it.hasNext()) {
                pdri = PDRIFactory.getFactory().createInstance(it.next(), false);
                reconnect = false;
            } else {
                reconnect = true;
            }
            if (pdri != null) {
                if (reconnect) {
                    pdri.reconnect();
                }
                pdri.copyRange(range, out);
//                if (!) {
//                    
//                } else {
//                    
//                    DesEncrypter encrypter = new DesEncrypter(pdri.getKeyInt());
//                    encrypter.decrypt(pdri.getData(), out);
//                }
            } else {
                sleepTime = 5;
                throw new NotFoundException("Physical resource not found");
            }
        } catch (IOException | java.lang.IllegalStateException ex) {
            if (!ex.getMessage().contains("does not support random reads")) {
                try {
                    sleepTime = sleepTime + 20;
                    Thread.sleep(sleepTime);
                    if (++tryCount < Constants.RECONNECT_NTRY) {
                        transfererRange(it, out, tryCount, pdri, range);
                    } else {
                        transfererRange(it, out, 0, null, range);
                    }
                } catch (InterruptedException ex1) {
                    sleepTime = 5;
                    throw new IOException(ex1);
                }
            } else {
                sleepTime = 5;
                throw new IOException(ex);
            }

        }
        sleepTime = 5;
        return pdri;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException, NotFoundException {
        double start = System.currentTimeMillis();
        PDRI pdri;
        Iterator<PDRIDescr> it;
        try {
            List<PDRIDescr> pdris = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId());
//            it = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId()).iterator();
            if (range != null) {
                if (range.getFinish() == null) {
                    range = new Range(range.getStart(), (getContentLength() - 1));
                }
                it = pdris.iterator();
                WebDataFileResource.log.log(Level.FINE, "Start: {0} end: {1} range: {2}", new Object[]{range.getStart(), range.getFinish(), range.getRange()});
                pdri = transfererRange(it, out, 0, null, range);
            } else {
//                pdri = transferer(it, out, 0, null, false);
                pdri = transferer(pdris, out, 0, doRedirect);
            }
        } catch (SQLException ex) {
            throw new BadRequestException(this, ex.getMessage());
        } catch (IOException ex) {
            if (ex.getMessage().contains("Resource not found")
                    || ex.getMessage().contains("Couldn't locate path")) {
                throw new NotFoundException(ex.getMessage());
            } else {
                throw new BadRequestException(this, ex.getMessage());
            }
        } finally {
            //Don't close the output, we need it to send back the response 
//            if (out != null) {
//                out.flush();
//                out.close();
//            }
        }
        double elapsed = System.currentTimeMillis() - start;
        long len;
        if (range != null) {
            len = range.getFinish() - range.getStart() + 1;
        } else {
            len = getContentLength();
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

        getCatalogue().addViewForRes(getLogicalData().getUid());
        String msg = "Source: " + pdri.getHost() + " Destination: " + fromAddress + " Tx_Speed: " + speed + " Kbites/sec Tx_Size: " + getContentLength() + " bytes";
        WebDataFileResource.log.log(Level.INFO, msg);
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {
        Set<String> keys = parameters.keySet();
//        for (String s : keys) {
//            WebDataFileResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, parameters.get(s)});
//        }
//
//        keys = files.keySet();
//        for (String s : keys) {
//            WebDataFileResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, files.get(s).getFieldName()});
//        }

        throw new BadRequestException(this, "Not implemented");
    }

    @Override
    public Date getCreateDate() {
        WebDataFileResource.log.log(Level.FINE, "getCreateDate() for {0}", getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public String checkRedirect(Request request) {
        try {
            switch (request.getMethod()) {
                case GET:
                    String redirect = null;

                    if (doRedirect) {

                        if (!canRedirect(request)) {
                            return null;
                        }
                        //Replica selection algorithm
//                        String from = ((HttpServletRequest) request).getRemoteAddr();
                        String from = request.getRemoteAddr();
                        redirect = getBestWorker(from);
                    }
                    WebDataFileResource.log.log(Level.INFO, "Redirecting to: {0}", redirect);
                    return redirect;
                default:
                    return null;
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException | IOException | InterruptedException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String getBestWorker(String reuSource) throws IOException, URISyntaxException, InterruptedException {
        if (doRedirect) {
//            if (uri != null) {
            if (PropertiesHelper.getSchedulingAlg().equals("traffic")) {
//                return getWorkerWithLessTraffic(reuSource);
                return getLowestCostWorker(reuSource);
            }
            if (PropertiesHelper.getSchedulingAlg().equals("round-robin")) {
                return getWorkerRoundRobin();
            }
            if (PropertiesHelper.getSchedulingAlg().equals("random")) {
                return getWorkerRandom();
            }
//            }

            workers = PropertiesHelper.getWorkers();
            if (workerIndex >= workers.size()) {
                workerIndex = 0;
            }

            String worker = workers.get(workerIndex++);
            String w = worker + "/" + getLogicalData().getUid();
            String token = UUID.randomUUID().toString();
            AuthWorker.setTicket(worker, token);
            return w + "/" + token;
        } else {
            return null;
        }
    }

    private boolean isInCache() throws SQLException, URISyntaxException, IOException {
        List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId());
        for (PDRIDescr pdri : pdriDescr) {
            if (pdri.getResourceUrl().startsWith("file")) {
                return true;
            }
        }

//        try (Connection cn = getCatalogue().getConnection()) {
//            List<PDRIDescr> pdriDescr = getCatalogue().getPdriDescrByGroupId(getLogicalData().getPdriGroupId(), cn);
//            for (PDRIDescr pdri : pdriDescr) {
//                if (pdri.getResourceUrl().startsWith("file")) {
//                    return true;
//                }
//            }
//        }
        return false;
    }

    private boolean canRedirect(Request request) throws SQLException, UnsupportedEncodingException, URISyntaxException, IOException {
        if (isInCache()) {
            return false;
        }
        Auth auth = request.getAuthorization();
        if (auth == null) {
            return false;
        }
        final String autheader = request.getHeaders().get("authorization");
        if (autheader != null) {
            final int index = autheader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index).getBytes()), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);
                if (authenticate(uname, token) == null) {
                    return false;
                }
                if (!authorise(request, Request.Method.GET, auth)) {
                    return false;
                }
            }
        }
        String userAgent = request.getHeaders().get("user-agent");
        if (userAgent == null || userAgent.length() <= 1) {
            return false;
        }
//        WebDataFileResource.log.log(Level.FINE, "userAgent: {0}", userAgent);
        List<String> nonRedirectableUserAgents = PropertiesHelper.getNonRedirectableUserAgents();
        for (String s : nonRedirectableUserAgents) {
            if (userAgent.contains(s)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean isBufferingRequired() {
        String res = ContentTypeUtils.findAcceptableContentType(getLogicalData().getContentTypesAsString(), null);
        for (String type : Constants.BUFFERED_TYPES) {
            if (res.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private String getLowestCostWorker(String reqSource) throws IOException, URISyntaxException, InterruptedException {
        if (sdnClient == null) {
            String uri = PropertiesHelper.getSDNControllerURL();
            sdnClient = new SDNControllerClient(uri);
        }
        String workerIP = sdnClient.getLowestCostWorker(reqSource, workersMap.keySet());
        String worker = workersMap.get(workerIP);
        String w = worker + "/" + getLogicalData().getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;

    }

    private String getWorkerWithLessTraffic(String reqSource) throws IOException, URISyntaxException {
//        if (sdnClient == null) {
//            String uri = PropertiesHelper.getSDNControllerURL();
//            sdnClient = new SDNControllerClient(uri);
//        }
//        Map<String, FloodlightStats> stats1 = sdnClient.getStatsMap();
//        try {
//            Thread.sleep(750);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Map<String, FloodlightStats> stats2 = sdnClient.getStatsMap();
//        Set<String> keys = stats1.keySet();
//        long cost = Long.MAX_VALUE;
//        String worker = workersMap.values().iterator().next();
//        for (String k : keys) {
//            FloodlightStats fs1 = stats1.get(k);
//
//            FloodlightStats fs2 = stats2.get(k);
//            long allStats = (fs2.receiveBytes - fs1.receiveBytes)
//                    + (fs2.transmitBytes - fs1.transmitBytes)
//                    + (fs2.receivePackets - fs1.receivePackets)
//                    + (fs2.transmitPackets - fs1.transmitPackets);
//            WebDataFileResource.log.log(Level.INFO, "worker: {0} cost: {1}", new Object[]{k, allStats});
//            if (allStats < cost && workersMap.containsKey(k)) {
//                cost = allStats;
//                worker = workersMap.get(k);
////                WebDataFileResource.log.log(Level.INFO, "worker: {0} cost: {1}", new Object[]{k, allStats});
//                if (cost <= 0) {
//                    break;
//                }
//            }
//
//        }
//
//        WebDataFileResource.log.log(Level.INFO, "portNum: {0} cost: {1} worker: {2}", new Object[]{portNum, cost, worker});
//        String w = worker + "/" + getLogicalData().getUid();
//        String token = UUID.randomUUID().toString();
//        AuthWorker.setTicket(worker, token);
//        int trans = 0;
//        if (numOfWorkerTransfersMap.containsKey(worker)) {
//            trans = numOfWorkerTransfersMap.get(worker);
//        }
//        trans++;
//        numOfWorkerTransfersMap.put(worker, trans);
////        WebDataFileResource.log.log(Level.INFO, "worker: {0} cost: {1} transfers: {2}", new Object[]{worker, cost, trans});
//        return w + "/" + token;
        return null;
    }

    private String getWorkerRoundRobin() throws IOException {
        workers = PropertiesHelper.getWorkers();
        if (workerIndex >= workers.size()) {
            workerIndex = 0;
        }
        String worker = workers.get(workerIndex++);
        String w = worker + "/" + getLogicalData().getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;
    }

    private String getWorkerRandom() throws IOException {
        workers = PropertiesHelper.getWorkers();
        int randomIndex = new Random().nextInt((workers.size() - 1 - 0) + 1) + 0;
        String worker = workers.get(randomIndex);
        String w = worker + "/" + getLogicalData().getUid();
        String token = UUID.randomUUID().toString();
        AuthWorker.setTicket(worker, token);
        return w + "/" + token;
    }
}

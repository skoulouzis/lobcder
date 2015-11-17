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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import static nl.uva.cs.lobcder.Util.ChacheEvictionAlgorithm.LRU;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIDescr;
import nl.uva.cs.lobcder.resources.PDRIFactory;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.cs.lobcder.rest.Endpoints;
import nl.uva.cs.lobcder.rest.wrappers.LogicalDataWrapped;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.rest.wrappers.StorageSiteWrapper;
import nl.uva.cs.lobcder.rest.wrappers.StorageSiteWrapperList;
import nl.uva.vlet.data.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 *
 * @author S. Koulouzis
 */
public class Catalogue {

    private final String restURL;
    private final String token;
    private final boolean sendStats;
    private final ClientConfig clientConfig;
    protected static final Map<String, LogicalDataWrapped> logicalDataCache = new HashMap<>();
    protected static final Map<Long, StorageSiteWrapper> storageSiteCache = new HashMap<>();
    protected static final Map<String, Long> fileAccessMap = new HashMap<>();
    protected static final Map<String, Double> weightPDRIMap = new HashMap<>();
    private Client restClient;
    private WebResource webResource;
    private File cacheFile;

    public Catalogue() throws IOException {
        restURL = Util.getRestURL();
        token = Util.getRestPassword();
        sendStats = Util.sendStats();
//            lim = Util.getRateOfChangeLim();
        //        uname = prop.getProperty(("rest.uname"));
        clientConfig = configureClient();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

    }

    private List<StorageSiteWrapper> getStorageSites(String id) throws URISyntaxException, UnknownHostException, SocketException, IOException {

        if (restClient == null) {
            restClient = Client.create(clientConfig);
            restClient.removeAllFilters();
            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
            webResource = restClient.resource(restURL);
        }
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("id", id);
        WebResource res = webResource.path("storage_sites").queryParams(params);

        StorageSiteWrapperList storageSiteList = res.accept(MediaType.APPLICATION_XML).
                get(new GenericType<StorageSiteWrapperList>() {
        });
        List<StorageSiteWrapper> ssites = removeUnreachableStorageSites(storageSiteList);

        for (StorageSiteWrapper ssw : ssites) {
            storageSiteCache.put(ssw.getStorageSiteId(), ssw);
        }
        return ssites;
    }

    private List<StorageSiteWrapper> removeUnreachableStorageSites(StorageSiteWrapperList sites) throws URISyntaxException, UnknownHostException, SocketException, IOException {
        List<StorageSiteWrapper> ssites = sites.getSites();
        if (ssites != null) {
            List<StorageSiteWrapper> removeIt = new ArrayList<>();

            for (StorageSiteWrapper p : sites.getSites()) {
                URI uri = new URI(p.getResourceURI());
                String pdriHost = uri.getHost();
                String pdriScheme = uri.getScheme();
                if (pdriHost == null || pdriHost.equals("localhost") || pdriHost.startsWith("127.0.")) {
                    removeIt.add(p);
                } else if (pdriScheme.equals("file") && !Catalogue.isPDRIOnWorker(new URI(p.getResourceURI()))) {
                    removeIt.add(p);
                }
            }
            if (!removeIt.isEmpty()) {
                ssites.removeAll(removeIt);
                if (ssites.isEmpty()) {
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.SEVERE, "PDRIS from master is either empty or contains unreachable files");
                    throw new IOException("PDRIS from master is either empty or contains unreachable files");
                }
            }
        }
        return ssites;
    }

    public void getPDRIs(String fileUID) throws IOException, URISyntaxException {
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
                    LogicalDataWrapped ld = removeUnreachablePDRIs(ldw, fileUID);
                    String uid = String.valueOf(ld.getLogicalData().getUid());
                    Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "Adding uid: {0}", uid);
                    Catalogue.logicalDataCache.put(uid, ld);
//                    for (PDRIDescr pdridecr : ld.getPdriList()) {
//                        storageSiteCache.put(pdridecr.getId(), pdridecr);
//                    }
                }
            }
        }
        if (Catalogue.storageSiteCache.isEmpty()) {
            getStorageSites("all");
        }
    }

    public PDRI getPDRI(String fileUID) throws InterruptedException, IOException, URISyntaxException {
        LogicalDataWrapped logicalData = getLogicalDataWrapped(fileUID);
        PDRIDescr pdriDesc = null;//new PDRIDescr();
        pdriDesc = selectBestPDRI(logicalData.getPdriList());
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Selected pdri: {0}", pdriDesc.getResourceUrl());

        return new VPDRI(pdriDesc.getName(), pdriDesc.getId(), pdriDesc.getResourceUrl(), pdriDesc.getUsername(),
                pdriDesc.getPassword(), pdriDesc.getEncrypt(), pdriDesc.getKey(), false);
    }

    public void setSpeed(Stats stats) throws JAXBException {
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

    private LogicalDataWrapped removeUnreachablePDRIs(LogicalDataWrapped logicalData, String fileUID) throws IOException, URISyntaxException {

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
                    Catalogue.logicalDataCache.remove(fileUID);
                    throw new IOException("PDRIS from master is either empty or contains unreachable files");
                }
                Catalogue.logicalDataCache.put(fileUID, logicalData);
            }
        }
        return logicalData;
    }

    public static boolean isPDRIOnWorker(URI pdriURI) throws URISyntaxException, UnknownHostException, SocketException {
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

    private PDRIDescr selectBestPDRI(List<PDRIDescr> pdris) throws URISyntaxException, UnknownHostException, SocketException {
        if (!pdris.isEmpty()) {
            Iterator<PDRIDescr> iter = pdris.iterator();
            while (iter.hasNext()) {
                PDRIDescr p = iter.next();
                URI uri = new URI(p.getResourceUrl());
                if (uri.getScheme().equals("file")) {
                    return p;
                }
                if (Catalogue.isPDRIOnWorker(uri)) {
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

    public void optimizeFlow(HttpServletRequest request) throws JAXBException {
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

    private LogicalDataWrapped addCacheFileToPDRIDescr(LogicalDataWrapped logicalData) throws IOException, URISyntaxException {
        List<PDRIDescr> pdris = logicalData.getPdriList();
        cacheFile = new File(Util.getCacheDir(), pdris.get(0).getName());
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
            boolean isCache = false;
            pdris.add(new PDRIDescr(fileName, ssID, resourceURI, uName, passwd, encrypt, BigInteger.valueOf(key), groupId, pdriId, isCache));
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

    private boolean isFileOnWorker(LogicalDataWrapped logicalData) throws URISyntaxException, UnknownHostException, SocketException {
        List<PDRIDescr> pdris = logicalData.getPdriList();
        for (PDRIDescr p : pdris) {
            if (isPDRIOnWorker(new URI(p.getResourceUrl()))) {
                return true;
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

    public LogicalDataWrapped getLogicalDataWrapped(String fileUID) throws IOException, URISyntaxException {
        LogicalDataWrapped logicalData = logicalDataCache.get(fileUID);
        Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Looking in cache for: {0}", fileUID);
        if (logicalData == null) {
            if (restClient == null) {
                restClient = Client.create(clientConfig);
                restClient.removeAllFilters();
                restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
                webResource = restClient.resource(restURL);
            }

            Logger.getLogger(WorkerServlet.class.getName()).log(Level.FINE, "Asking master. Token: {0} fileID: " + fileUID + " logicalDataCache.size: " + logicalDataCache.size(), token);
            WebResource res = webResource.path("item").path("query").path(fileUID);
            logicalData = res.accept(MediaType.APPLICATION_XML).
                    get(new GenericType<LogicalDataWrapped>() {
            });
            logicalData = removeUnreachablePDRIs(logicalData, fileUID);
        }

        logicalData = addCacheFileToPDRIDescr(logicalData);
        logicalDataCache.put(fileUID, logicalData);
        return logicalData;
    }

//    protected boolean replicate(Long pdriGroupId, Collection<Long> toReplicate) {
//        if (toReplicate.isEmpty()) {
////            log.log(Level.FINE, "toReplicate.isEmpty()");
//            return true;
//        }
//        boolean result = true;
//        try (PreparedStatement preparedStatement = connection.prepareStatement(
//                "INSERT INTO pdri_table (fileName, storageSiteRef, pdriGroupRef, "
//                + "isEncrypted, encryptionKey) VALUES (?,?,?,?,?)")) {
//            
//            PDRIDescr sourceDescr = getSourcePdriDescrForGroup(pdriGroupId);
//            PDRI sourcePdri = PDRIFactory.getFactory().createInstance(sourceDescr);
//
//            for (Long site : toReplicate) {
//                try {
//                    StorageSite ss = getStorageSiteById(site, connection);
//                    BigInteger pdriKey = nl.uva.cs.lobcder.util.DesEncrypter.generateKey();
//                    PDRIDescr destinationDescr = new PDRIDescr(
//                            generateFileName(sourceDescr),
//                            ss.getStorageSiteId(),
//                            ss.getResourceURI(),
//                            ss.getCredential().getStorageSiteUsername(),
//                            ss.getCredential().getStorageSitePassword(),
//                            ss.isEncrypt(),
//                            pdriKey,
//                            pdriGroupId,
//                            null, ss.isCache());
//                    PDRI destinationPdri = PDRIFactory.getFactory().createInstance(destinationDescr);
//                    destinationPdri.replicate(sourcePdri);
//                    result = destinationPdri.exists(destinationPdri.getFileName());
//                    long srcLen = sourcePdri.getLength();
//                    if (result == false || destinationPdri.getLength() != srcLen) {
//                        log.log(Level.WARNING, "Failed to replicate {0}/{1} to {2}/{3}", new Object[]{sourcePdri.getURI(), sourcePdri.getFileName(), destinationPdri.getURI(), destinationPdri.getFileName()});
//                        result = false;
//                    }
////                    else {
//                    preparedStatement.setString(1, destinationDescr.getName());
//                    preparedStatement.setLong(2, destinationDescr.getStorageSiteId());
//                    preparedStatement.setLong(3, destinationDescr.getPdriGroupRef());
//                    preparedStatement.setBoolean(4, destinationDescr.getEncrypt());
//                    preparedStatement.setLong(5, destinationDescr.getKey().longValue());
//                    preparedStatement.executeUpdate();
////                    }
//
//                } catch (Exception e) {
//                    result = false;
//                } catch (Throwable e) {
//                    result = false;
//                }
//            }
//            return result;
//        } catch (Exception e) {
//            return false;
//        }
//    }
    boolean replicate(Pair<File, String> p) throws IOException {
        boolean result = true;

        PDRIDescr sourceDescr = createPdriDescr(p.getLeft());
        PDRI sourcePdri = PDRIFactory.getFactory().createInstance(sourceDescr);
        String[] sites = p.getRight().split("-");
        Long fileUID = Long.valueOf(sites[0]);
        Long pdriGroupId = Long.valueOf(sites[1]);
        for (int i = 2; i < sites.length; i++) {
            try {
                StorageSiteWrapper ss = storageSiteCache.get(Long.valueOf(sites[i]));
                if (ss == null) {
                    getStorageSites(String.valueOf(Long.valueOf(sites[i])));
                    ss = storageSiteCache.get(Long.valueOf(sites[i]));
                }
                BigInteger pdriKey = nl.uva.cs.lobcder.util.DesEncrypter.generateKey();
                PDRIDescr destinationDescr = new PDRIDescr(
                        sourceDescr.getName(),
                        ss.getStorageSiteId(),
                        ss.getResourceURI(),
                        ss.getCredential().getStorageSiteUsername(),
                        ss.getCredential().getStorageSitePassword(),
                        ss.isEncrypt(),
                        pdriKey,
                        pdriGroupId,
                        null, ss.isCache());

                PDRI destinationPdri = PDRIFactory.getFactory().createInstance(destinationDescr);
                destinationPdri.replicate(sourcePdri);
                result = destinationPdri.exists(destinationPdri.getFileName());
                long srcLen = sourcePdri.getLength();
                if (result == false || destinationPdri.getLength() != srcLen) {
                    result = false;
                }
                result = updateMaster(destinationDescr);
            } catch (Exception e) {
                result = false;
            } catch (Throwable e) {
                result = false;
            }
        }
        return result;
    }

    private PDRIDescr createPdriDescr(File key) throws IOException {
        PDRIDescr pDRIDescr = new PDRIDescr();
        pDRIDescr.setCashe(true);
        pDRIDescr.setEncrypt(false);
//        file:/home/alogo/servers/apache-tomcat-6.0.36/temp/uploads/LOBCDER-REPLICA-vTEST
        String localDir = key.getParentFile().toURI().toString().replaceAll(Util.getWorkingFolderName() + "/", "");
        pDRIDescr.setResourceUrl(localDir);
        pDRIDescr.setName(key.getName());
        pDRIDescr.setPassword("fake");
        pDRIDescr.setUsername("fake");
        return pDRIDescr;
    }

    private boolean updateMaster(PDRIDescr destinationDescr) {
        if (restClient == null) {
            restClient = Client.create(clientConfig);
            restClient.removeAllFilters();
            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter("worker-", token));
            webResource = restClient.resource(restURL);
        }
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        return true;
    }
}

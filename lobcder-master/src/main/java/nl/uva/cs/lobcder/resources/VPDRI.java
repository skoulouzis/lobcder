/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import io.milton.http.Range;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.GridHelper;
import nl.uva.cs.lobcder.util.Network;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.vfs.*;
import nl.uva.vlet.vfs.cloud.CloudFile;
import nl.uva.vlet.vfs.webdavfs.WebdavFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRSContext;
import nl.uva.vlet.vrs.io.VRandomReadable;

/**
 * A test PDRI to implement the delete get/set data methods with the VRS API
 *
 * @author S. koulouzis
 */
@Log
public class VPDRI implements PDRI {

    static {
        try {
            GridHelper.InitGlobalVFS();
        } catch (Exception ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private VFSClient vfsClient;
    private VRL vrl;
    private final String username;
    private final String password;
    private final Long storageSiteId;
    private static String baseDir; //= "LOBCDER-REPLICA-vTEST";//"LOBCDER-REPLICA-v2.0";
    private final String fileName;
    private int reconnectAttemts = 0;
    private BigInteger keyInt;
    private boolean encrypt;
    private final String resourceUrl;
    private boolean doChunked;
    private int sleeTime = 5;
//    private static final Map<String, GridProxy> proxyCache = new HashMap<>();
    private boolean destroyCert;
    private long length = -1;
    private Client restClient;
    private static final String restURL = "http://localhost:8080/lobcder/rest/";
    private boolean isCache = false;

    public VPDRI(String fileName, Long storageSiteId, String resourceUrl,
            String username, String password, boolean encrypt, BigInteger keyInt,
            boolean doChunkUpload) throws IOException {
        try {
            this.fileName = fileName;
            this.resourceUrl = resourceUrl.replaceAll(" ", "");
            baseDir = nl.uva.cs.lobcder.util.PropertiesHelper.getBackendWorkingFolderName();
//            String encoded = VRL.encode(fileName);
            vrl = new VRL(this.resourceUrl).appendPath(baseDir).append(fileName);
//            vrl= new VRL(vrl.toNormalizedString());
//            vrl = new VRL(resourceUrl).appendPath(baseDir).append(URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
            this.storageSiteId = storageSiteId;
            this.username = username;
            this.password = password;
            this.encrypt = encrypt;
            this.keyInt = keyInt;
            this.doChunked = doChunkUpload;
            initVFS();
//            initRESTClient();
//            VPDRI.log.log(Level.FINE, "Done init. fileName: {0}, storageSiteId: {1}, username: {2}, password: {3}, VRL: {4}", new Object[]{fileName, storageSiteId, "username", "password", vrl});
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private void initVFS() throws Exception {
        this.vfsClient = new VFSClient();
        VRSContext context = this.getVfsClient().getVRSContext();
        context.setProperty(GlobalConfig.TCP_CONNECTION_TIMEOUT, "20000");
        //Bug in sftp: We have to put the username in the url
        ServerInfo info = context.getServerInfoFor(vrl, true);
        String authScheme = info.getAuthScheme();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH)) {
            GridHelper.initGridProxy(username, password, context, destroyCert);
//            copyVomsAndCerts();
//            GridProxy gridProxy = context.getGridProxy();
//            if (destroyCert) {
//                gridProxy.destroy();
//                gridProxy = null;
//            }
//            if (gridProxy == null || gridProxy.isValid() == false) {
//                context.setProperty("grid.proxy.location", Constants.PROXY_FILE);
//                // Default to $HOME/.globus
//                context.setProperty("grid.certificate.location", Global.getUserHome() + "/.globus");
//                String vo = username;
//                context.setProperty("grid.proxy.voName", vo);
//                context.setProperty("grid.proxy.lifetime", "200");
//                gridProxy = context.getGridProxy();
//                if (gridProxy.isValid() == false) {
//                    gridProxy.setEnableVOMS(true);
//                    gridProxy.setDefaultVOName(vo);
//                    gridProxy.createWithPassword(password);
//                    if (gridProxy.isValid() == false) {
//                        throw new VlException("Created Proxy is not Valid!");
//                    }
//                    gridProxy.saveProxyTo(Constants.PROXY_FILE);
////                    proxyCache.put(password, gridProxy);
//                }
//            }
        }

        if (StringUtil.equals(authScheme, ServerInfo.PASSWORD_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSPHRASE_AUTH)) {
//            String username = storageSite.getCredential().getStorageSiteUsername();
            if (username == null) {
                throw new NullPointerException("Username is null!");
            }
            info.setUsername(username);
//            String password = storageSite.getCredential().getStorageSitePassword();
            if (password == null) {
                throw new NullPointerException("password is null!");
            }
            info.setPassword(password);
        }

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);

//        if(getVrl().getScheme().equals(VRS.SFTP_SCHEME)){
        //patch for bug with ssh driver 
        info.setAttribute("sshKnownHostsFile", System.getProperty("user.home") + "/.ssh/known_hosts");
//        }
        context.setProperty("chunk.upload", doChunked);
//        info.setAttribute(new VAttribute("chunk.upload", true));
        info.store();

    }

    @Override
    public void delete() throws IOException {
        try {
            VPDRI.log.log(Level.INFO, "Start deleting {0}", new Object[]{vrl});
//            getVfsClient().openLocation(vrl).delete();
            getVfsClient().getFile(vrl).delete();
        } catch (VlException ex) {
            //Maybe it's from assimilation. We must remove the baseDir
            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("Couldn open location. Get NULL object for location")) {
                try {
                    // VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
//                    String encoded = VRL.encode(fileName);
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
                    getVfsClient().getFile(assimilationVRL).delete();
//                    getVfsClient().openLocation(assimilationVRL).delete();

                } catch (IOException | VlException ex1) {
                    Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } else {
                Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
        }
        //        //it's void so do it asynchronously
        //        Runnable asyncDel = getAsyncDelete(this.vfsClient, vrl);
        //        asyncDel.run();
    }

    @Override
    public void copyRange(Range range, OutputStream out) throws IOException {
        VFile file;
        try {
//            file = (VFile) getVfsClient().openLocation(vrl);
            file = getVfsClient().getFile(vrl);
            doCopy(file, range, out, getEncrypted());
        } catch (Exception ex) {
            if (ex instanceof ResourceNotFoundException
                    || ex.getMessage().contains("Couldn open location. Get NULL object for location:")) {
                try {
//                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8"));
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
//                    file = (VFile) getVfsClient().openLocation(assimilationVRL);
                    file = getVfsClient().getFile(assimilationVRL);
                    doCopy(file, range, out, getEncrypted());

                    sleeTime = 5;
                } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | VRLSyntaxException ex1) {
                    throw new IOException(ex1);
                } catch (VlException ex1) {
                    if (reconnectAttemts < Constants.RECONNECT_NTRY) {
                        try {
                            sleeTime = sleeTime + 5;
                            Thread.sleep(sleeTime);
                            reconnect();
                            copyRange(range, out);
                        } catch (InterruptedException ex2) {
                            throw new IOException(ex1);
                        }
                    } else {
                        throw new IOException(ex1);
                    }
                }
            } else if (reconnectAttemts < Constants.RECONNECT_NTRY && !ex.getMessage().contains("does not support random reads")) {
                try {
                    sleeTime = sleeTime + 5;
                    Thread.sleep(sleeTime);
                    reconnect();
                    copyRange(range, out);
                } catch (InterruptedException ex1) {
                    throw new IOException(ex);
                }
            } else {
                throw new IOException(ex);
            }
        } finally {
        }
    }

    private void doCopy(VFile file, Range range, OutputStream out, boolean decript) throws VlException, IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        long len = range.getFinish() - range.getStart() + 1;
        InputStream in = null;
        int buffSize;
        Long start = range.getStart();
        if (len <= Constants.BUF_SIZE) {
            buffSize = (int) len;
        } else {
            buffSize = Constants.BUF_SIZE;
        }
        DesEncrypter en = null;
        if (decript) {
            en = new DesEncrypter(getKeyInt());
        }

        int read = 0;
        try {
            if (file instanceof VRandomReadable) {
                VRandomReadable ra = (VRandomReadable) file;
                byte[] buff = new byte[buffSize];
                int totalBytesRead = 0;
                while (totalBytesRead < len || read != -1) {
                    long startT = System.currentTimeMillis();
                    read = ra.readBytes(start, buff, 0, buff.length);
                    VPDRI.log.log(Level.INFO, "speed: {0} kb/s", (read / 1024.0) / ((System.currentTimeMillis() - startT) / 1000.0));
                    if (read == -1 || totalBytesRead == len) {
                        break;
                    }
                    totalBytesRead += read;
                    start += buff.length;
                    if (decript) {
                        byte[] tmp = en.decrypt(buff);
                        buff = tmp;
                    }
                    out.write(buff, 0, read);
                }
            } else {
                if (start > 0) {
                    throw new IOException("Backend at " + vrl.getScheme() + "://" + vrl.getHostname() + "does not support random reads");
//                    long skiped = in.skip(start);
//                    if (skiped != start) {
//                        long n = start;
//                        int buflen = (int) Math.min(Constants.BUF_SIZE, n);
//                        byte data[] = new byte[buflen];
//                        while (n > 0) {
//                            int r = in.read(data, 0, (int) Math.min((long) buflen, n));
//                            if (r < 0) {
//                                break;
//                            }
//                            n -= r;
//                        }
//                    }
                }
//                int totalBytesRead = 0;
//                byte[] buff = new byte[buffSize];
//                while (totalBytesRead < len || read != -1) {
//                     if (read == -1 || totalBytesRead == len) {
//                        break;
//                    }
//                    read = in.read(buff, 0, buff.length);
//                    totalBytesRead += read;
//                    start += buff.length;
//                    out.write(buff, 0, read);
//                }
                in = getData();
                if (decript) {
                    InputStream tmp = en.wrapInputStream(in);
                    in = tmp;
                }
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer(buffSize, in, out);
                cBuff.startTransfer(len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    public InputStream getData() throws IOException {
        InputStream in = null;
        VFile file;
        int read;
        try {
//            file = (VFile) getVfsClient().openLocation(vrl);
            file = getVfsClient().getFile(vrl);
            in = file.getInputStream();
        } catch (Exception ex) {
            if (ex instanceof ResourceNotFoundException
                    || ex.getMessage().contains("Couldn open location. Get NULL object for location:")) {
                try {
//                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8"));
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
//                    in = ((VFile) getVfsClient().openLocation(assimilationVRL)).getInputStream();
                    in = (getVfsClient().getFile(assimilationVRL)).getInputStream();
                    sleeTime = 5;
                } catch (VRLSyntaxException ex1) {
                    throw new IOException(ex1);
                } catch (VlException ex1) {
                    if (ex instanceof ResourceNotFoundException) { //|| ex.getMessage().contains("Couldn open location. Get NULL object for location:")) {
                        throw new IOException(ex1);
                    }
                    if (reconnectAttemts < Constants.RECONNECT_NTRY) {
                        try {
                            sleeTime = sleeTime + 5;
                            Thread.sleep(sleeTime);
                            reconnect();
                            return getData();
                        } catch (InterruptedException ex2) {
                            throw new IOException(ex1);
                        }
                    } else {
                        throw new IOException(ex1);
                    }
                }
            } else if (reconnectAttemts < Constants.RECONNECT_NTRY-2) {
                if (ex instanceof org.globus.common.ChainedIOException) {
                    destroyCert = true;
                }
                try {
                    reconnect();
                    sleeTime = sleeTime + 5;
                    Thread.sleep(sleeTime);
                    return getData();
                } catch (InterruptedException ex1) {
                    throw new IOException(ex);
                }
            } else {
                throw new IOException(ex);
            }
        } finally {
//            reconnect();
        }
        return in;
    }

    @Override
    public void putData(InputStream in) throws IOException {
        OutputStream out = null;
        double start = System.currentTimeMillis();
//        VFile tmpFile = null;
        try {
            //            upload(in);
            VRL parentVrl = vrl.getParent();
            VDir remoteDir = getVfsClient().mkdirs(parentVrl, true);
            getVfsClient().createFile(vrl, true);
            out = getVfsClient().getFile(vrl).getOutputStream();

            if (!getEncrypted()) {
//                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
//                cBuff.startTransfer(new Long(-1));
                int read;
                byte[] copyBuffer = new byte[Constants.BUF_SIZE];
                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }

            } else {
                DesEncrypter encrypter = new DesEncrypter(getKeyInt());
                encrypter.encrypt(in, out);
            }
            reconnectAttemts = 0;

        } catch (nl.uva.vlet.exception.VlAuthenticationException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new IOException(ex);
        } catch (VlException ex) {
            if (ex.getMessage() != null) {
                VPDRI.log.log(Level.FINE, "\tVlException {0}", ex.getMessage());
            }
            if (reconnectAttemts <= 2) {
                VPDRI.log.log(Level.FINE, "\treconnectAttemts {0}", reconnectAttemts);
                reconnect();
                putData(in);
            } else {
                throw new IOException(ex);
            }
//            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("not found") || ex.getMessage().contains("Couldn open location") || ex.getMessage().contains("not found in container")) {
//                try {
//                    vfsClient.mkdirs(vrl.getParent(), true);
//                    vfsClient.createFile(vrl, true);
//                    putData(in);
//                } catch (VlException ex1) {
//                    throw new IOException(ex1);
//                }
//            }
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (java.io.IOException ex) {
                }
            }
            if (in != null) {
                in.close();
            }
        }
        double elapsed = System.currentTimeMillis() - start;
        double speed = ((getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
        try {
            String msg = "File: " + this.getFileName() + " Destination: " + new URI(getURI()).getScheme() + "://" + getHost() + " Rx_Speed: " + speed + " Kbites/sec Rx_Size: " + (getLength()) + " bytes Elapsed_Time: " + elapsed + " ms";
            VPDRI.log.log(Level.INFO, msg);
        } catch (URISyntaxException ex) {
        }

    }

    @Override
    public Long getStorageSiteId() {
        return this.storageSiteId;//storageSite.getStorageSiteId();
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String getHost() throws UnknownHostException {
        if (vrl.getScheme().equals("file")
                || StringUtil.isEmpty(vrl.getHostname())
                || vrl.getHostname().equals("localhost")
                || vrl.getHostname().equals("127.0.0.1")) {
            return Network.getIP("localhost");
        } else {
            return vrl.getHostname();
        }
    }

    @Override
    public long getLength() throws IOException {
        try {
            if (length > -1) {
                return length;
            }
            return getVfsClient().getFile(vrl).getLength();
        } catch (Exception ex) {
            if(ex.getMessage().contains("Couldn open location.") && ex.getMessage().contains("%")){
                try {
                    return getVfsClient().getFile( new VRL(vrl.toNormalizedString())).getLength();
//                    this.vrl = new VRL(vrl.toNormalizedString());
//                     getLength();
                } catch (VRLSyntaxException ex1) {
                    Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (VlException ex1) {
                    Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            if (reconnectAttemts < Constants.RECONNECT_NTRY) {
                reconnect();
                getLength();
            } else {
                throw new IOException(ex);
            }
        } finally {
        }
        return 0;
    }

    @Override
    public void reconnect() throws IOException {
        reconnectAttemts++;
        getVfsClient().close();
        getVfsClient().dispose();
//        VRS.exit();
        try {
            initVFS();
        } catch (Exception ex1) {
            throw new IOException(ex1);
        }
    }

    @Override
    public Long getChecksum() throws IOException {
        try {
            VFile physicalFile = getVfsClient().getFile(vrl);
            if (physicalFile instanceof VChecksum) {
                BigInteger bi = new BigInteger(((VChecksum) physicalFile).getChecksum("MD5"), 16);
                return bi.longValue();
            }
        } catch (VlException ex) {
            throw new IOException(ex);
        } finally {
        }
        return null;
    }

    @Override
    public String getStringChecksum() throws IOException {
        try {
            VFile physicalFile = getVfsClient().getFile(vrl);
            if (physicalFile instanceof VChecksum) {
                return ((VChecksum) physicalFile).getChecksum("MD5");
            }
        } catch (VlException ex) {
            throw new IOException(ex);
        } finally {
        }
        return null;
    }

//    private void debug(String msg) {
//        if (debug) {
////            System.err.println(this.getClass().getName() + ": " + msg);
//            log.debug(msg);
//        }
//    }
    private void upload(PDRI source) throws VlException, IOException {
        if (getVfsClient() == null) {
            reconnect();
        }
        VFile file = getVfsClient().createFile(vrl, true);
        if (file instanceof CloudFile) {
            CloudFile uFile = (CloudFile) file;
            VFile sourceFile = getVfsClient().openFile(new VRL(source.getURI()));
            uFile.uploadFrom(sourceFile);
        }else if (file instanceof WebdavFile) {
            WebdavFile wFile = (WebdavFile) file;
            VFile sourceFile = getVfsClient().openFile(new VRL(source.getURI()));
            wFile.uploadFrom(sourceFile);
        }
    }

    @Override
    public void replicate(PDRI source) throws IOException {
        try {

            VRL sourceVRL = new VRL(source.getURI());
            String sourceScheme = sourceVRL.getScheme();
            String desteScheme = vrl.getScheme();
            VPDRI.log.log(Level.INFO, "Start replicating {0} to {1}", new Object[]{source.getURI(), getURI()});
            double start = System.currentTimeMillis();
            if (!vfsClient.existsDir(vrl.getParent())) {
                VDir remoteDir = vfsClient.mkdirs(vrl.getParent(), true);
            }
            if (desteScheme.equals("swift") && sourceScheme.equals("file") || desteScheme.startsWith("webdav") && sourceScheme.equals("file")) {
                upload(source);
            } else {
                VFile destFile = getVfsClient().createFile(vrl, true);
                VFile sourceFile = getVfsClient().openFile(sourceVRL);
                getVfsClient().copy(sourceFile, destFile);
            }
//            putData(source.getData());
            double elapsed = System.currentTimeMillis() - start;
            double speed = ((source.getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);

            Stats stats = new Stats();
            stats.setSource(source.getHost());
            stats.setDestination(getHost());
            stats.setSpeed(speed);
            stats.setSize(getLength());
            try {
                setSpeed(stats);
            } catch (JAXBException ex) {
                Logger.getLogger(VPDRI.class.getName()).log(Level.WARNING, null, ex);
            }


            String msg = "Source: " + source.getHost() + " Destination: " + vrl.getScheme() + "://" + getHost() + " Replication_Speed: " + speed + " Kbites/sec Repl_Size: " + (getLength()) + " bytes";
            VPDRI.log.log(Level.INFO, msg);


//            getAsyncDelete(getVfsClient(), vrl).run();

        } catch (VlException ex) {
            throw new IOException(ex);
        }
    }

//    @Override
//    public void setKeyInt(BigInteger keyInt) {
//        this.keyInt = keyInt;
//    }
//
    @Override
    public BigInteger getKeyInt() {
        return this.keyInt;
    }

    @Override
    public boolean getEncrypted() {
        return this.encrypt;
    }

//
//    @Override
//    public void replicate(PDRI source) throws IOException {
//        try {
//            VRL sourceVRL = new VRL(source.getURI());
////            VRL destVRL = this.vrl.getParent();
//            log.debug("replicate from "+sourceVRL+" to "+vrl);
//            VFile sourceFile = this.vfsClient.openFile(sourceVRL);
//            VFile destFile = this.vfsClient.createFile(vrl, true);
//            if (destFile instanceof CloudFile) {
//                ((CloudFile) destFile).uploadFrom(sourceFile);
//            } else {
//                putData(source.getData());
//            }
//        } catch (VlException ex) {
//            throw new IOException(ex);
//        }
//    }
    @Override
    public String getURI() throws IOException {
        try {
            return this.vrl.toURIString();
        } catch (VRLSyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * @return the vfsClient
     */
    private VFSClient getVfsClient() throws IOException {
        if (vfsClient == null) {
            reconnect();
        }
        return vfsClient;
    }

    @Override
    public void setLength(long length) {
        this.length = length;
    }

    @Override
    public boolean exists(String fileName) throws IOException {
        try {
            VRL vrlss = vrl.getParent().append(fileName);
            return getVfsClient().existsFile(vrlss);
        } catch (VlException ex) {
            throw new IOException(ex);
        }
    }

    private void setSpeed(Stats stats) throws JAXBException {
        if (this.restClient != null) {
            JAXBContext context = JAXBContext.newInstance(Stats.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            OutputStream out = new ByteArrayOutputStream();
            m.marshal(stats, out);

            WebResource webResource = restClient.resource(restURL);
            String stringStats = String.valueOf(out);
            ClientResponse response = webResource.path("lob_statistics").path("set")
                    .type(MediaType.APPLICATION_XML).put(ClientResponse.class, stringStats);
        }

    }

    public static ClientConfig configureClient() {
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

    @Override
    public boolean isCahce() {
        return isCache;
    }

    void setIsCahce(boolean isCache) {
        this.isCache = isCache;
    }

    private void initRESTClient() {
        try {
            ClientConfig clientConfig = configureClient();
//        SSLContext ctx = SSLContext.getInstance("SSL");
//        clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, ctx));
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            restClient = Client.create(clientConfig);
            restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(getClass().getName(), PropertiesHelper.getLobComponentToken()));
        } catch (IOException ex) {
            //Not important. Just continue 
            Logger.getLogger(VPDRI.class.getName()).log(Level.WARNING, "Failed to initilize REST client", ex);
            restClient = null;
        }
    }
}

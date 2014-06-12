/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.java.Log;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.vfs.*;
import nl.uva.vlet.vfs.cloud.CloudFile;
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
    //    private MyStorageSite storageSite;
    private VRL vrl;
    private final String username;
    private final String password;
    private final Long storageSiteId;
    private final String baseDir = "LOBCDER-REPLICA-vTEST";//"LOBCDER-REPLICA-v2.0";
    private final String fileName;
    private int reconnectAttemts = 0;
    private final static boolean debug = true;
    private BigInteger keyInt;
    private boolean encrypt;
    private final String resourceUrl;
    private boolean doChunked;
    private int sleeTime = 5;
//    private static final Map<String, GridProxy> proxyCache = new HashMap<>();
    private boolean destroyCert;
    private final int bufferSize;
    private final boolean qosCopy;
    private final double lim;

    public VPDRI(String fileName, Long storageSiteId, String resourceUrl,
            String username, String password, boolean encrypt, BigInteger keyInt,
            boolean doChunkUpload) throws IOException {
        try {
            this.fileName = fileName;
            if (this.fileName == null) {
                throw new NullPointerException("fileName is null");
            }
            this.resourceUrl = resourceUrl;
            if (this.resourceUrl == null) {
                throw new NullPointerException("resourceUrl is null");
            }
            String encoded = VRL.encode(fileName);
            if (encoded == null) {
                throw new NullPointerException("encoded is null");
            }
            vrl = new VRL(resourceUrl).appendPath(baseDir).append(encoded);
            if (this.vrl == null) {
                throw new NullPointerException("vrl is null");
            }
            this.storageSiteId = storageSiteId;
            if (this.storageSiteId == null) {
                throw new NullPointerException("storageSiteId is null");
            }
            this.username = username;
            if (this.username == null) {
                throw new NullPointerException("username is null");
            }
            this.password = password;
            if (this.password == null) {
                throw new NullPointerException("password is null");
            }
            this.encrypt = encrypt;

            this.keyInt = keyInt;
            this.doChunked = doChunkUpload;
            Logger.getLogger(VPDRI.class.getName()).log(Level.FINE, "fileName: {0}, storageSiteId: {1}, username: {2}, password: {3}, VRL: {4}", new Object[]{fileName, storageSiteId, username, password, vrl});
            if (vrl.getScheme().equals("file")) {
            }
            initVFS();
            bufferSize = Util.getBufferSize();
            qosCopy = Util.doQosCopy();
            lim = Util.getRateOfChangeLim();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private void initVFS() {
        try {
            this.vfsClient = new VFSClient();
            VRSContext context = this.getVfsClient().getVRSContext();
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
        } catch (IOException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VlException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delete() throws IOException {
        try {
            getVfsClient().openLocation(vrl).delete();
        } catch (VlException ex) {
            //Maybe it's from assimilation. We must remove the baseDir
            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("Couldn open location. Get NULL object for location")) {
                try {
                    //                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
//                    String encoded = VRL.encode(fileName);
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
                    getVfsClient().openLocation(assimilationVRL).delete();

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
    public InputStream getData() throws IOException {
        InputStream in = null;
        VFile file;
        int read;
        try {
            file = (VFile) getVfsClient().openLocation(vrl);
            in = file.getInputStream();
        } catch (Exception ex) {
            if (ex instanceof ResourceNotFoundException
                    || ex.getMessage().contains("Couldn open location. Get NULL object for location:")
                    || ex.getMessage().contains("Resource not found.:Couldn't locate path")) {
                try {
//                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8"));
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
                    in = ((VFile) getVfsClient().openLocation(assimilationVRL)).getInputStream();
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
            } else if (reconnectAttemts < Constants.RECONNECT_NTRY) {
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
        Logger.getLogger(VPDRI.class.getName()).log(Level.FINE, "putData:");
//        VFile tmpFile = null;
        try {
            //            upload(in);
            VRL parentVrl = vrl.getParent();
            VDir remoteDir = getVfsClient().mkdirs(parentVrl, true);
            getVfsClient().createFile(vrl, true);
            out = getVfsClient().getFile(vrl).getOutputStream();
            if (!getEncrypted()) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((bufferSize), in, out);
                cBuff.startTransfer(new Long(-1));
//                int read;
//                byte[] copyBuffer = new byte[Constants.BUF_SIZE];
//                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
//                    out.write(copyBuffer, 0, read);
//                }

            } else {
                DesEncrypter encrypter = new DesEncrypter(getKeyInt());
                encrypter.encrypt(in, out);
            }
            reconnectAttemts = 0;

        } catch (nl.uva.vlet.exception.VlAuthenticationException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new IOException(ex);
        } catch (VlException ex) {
            if (ex.getMessage() != null) {
                Logger.getLogger(VPDRI.class.getName()).log(Level.FINE, "\tVlException {0}", ex.getMessage());
            }
            if (reconnectAttemts <= 2) {
                Logger.getLogger(VPDRI.class.getName()).log(Level.FINE, "\treconnectAttemts {0}", reconnectAttemts);
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
            return getIP("localhost");
        } else {
            return vrl.getHostname();
        }
    }

    private String getIP(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException ex) {
            return hostName;
        }
    }

    @Override
    public long getLength() throws IOException {
        try {
            return getVfsClient().getFile(vrl).getLength();
        } catch (Exception ex) {
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
        CloudFile thisFile = (CloudFile) getVfsClient().createFile(vrl, true);
        VFile sourceFile = getVfsClient().openFile(new VRL(source.getURI()));
        thisFile.uploadFrom(sourceFile);
    }

    @Override
    public void replicate(PDRI source) throws IOException {
        try {

            VRL sourceVRL = new VRL(source.getURI());
            String sourceScheme = sourceVRL.getScheme();
            String desteScheme = vrl.getScheme();
            Logger.getLogger(VPDRI.class.getName()).log(Level.INFO, "Start replicating {0} to {1}", new Object[]{source.getURI(), getURI()});
            double start = System.currentTimeMillis();
            if (!vfsClient.existsDir(vrl.getParent())) {
                VDir remoteDir = vfsClient.mkdirs(vrl.getParent(), true);
            }
            if (desteScheme.equals("swift") && sourceScheme.equals("file")) {
                upload(source);
            } else {
                VFile destFile = getVfsClient().createFile(vrl, true);
                VFile sourceFile = getVfsClient().openFile(sourceVRL);
                getVfsClient().copy(sourceFile, destFile);
            }
//            putData(source.getData());
            double elapsed = System.currentTimeMillis() - start;
            double speed = ((source.getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);

            String msg = "Source: " + source.getHost() + " Destination: " + vrl.getScheme() + "://" + getHost() + " Replication_Speed: " + speed + " Kbites/sec Repl_Size: " + (getLength()) + " bytes";
            Logger.getLogger(VPDRI.class.getName()).log(Level.INFO, msg);


//            getAsyncDelete(getVfsClient(), vrl).run();

        } catch (VlException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
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
    public void copyRange(OutputStream output, long start, long length) throws IOException {
        try {
            VFile file = (VFile) getVfsClient().openLocation(vrl);
            if (file instanceof VRandomReadable) {
                VRandomReadable ra = (VRandomReadable) file;
                // Write partial range.
                byte[] buffer = new byte[bufferSize];
                int read;
                long toRead = length;
                long total = 0;
                double speed;
                double rateOfChange = 0;
                double speedPrev = 0;
                long startTime = System.currentTimeMillis();
                while ((read = ra.readBytes(start, buffer, 0, buffer.length)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                        start += read;
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        start += read;
                        break;
                    }
                    total += read;
                    double progress = (100.0 * total) / length;
                    if (progress % 10 == 0 && progress > 10) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        speed = (total / elapsed);
                        rateOfChange = (speed - speedPrev);
                        speedPrev = speed;
                        Logger.getLogger(WorkerServlet.class.getName()).log(Level.INFO, "speed: {0} rateOfChange: {1}", new Object[]{speed, rateOfChange});
                        if (rateOfChange < lim && progress > 12) {
                            //This works with export ec=18; while [ $ec -eq 18 ]; do curl -O -C - -L --request GET -u user:pass http://localhost:8080/lobcder/dav/large_file; export ec=$?; done
                            Logger.getLogger(WorkerServlet.class.getName()).log(Level.WARNING, "We will not tolarate this !!!! Find a new worker. rateOfChange: " + rateOfChange);
                            break;
                        }
                    }
                }
            } else {
                throw new IOException("Backend at " + vrl.getScheme() + "://" + vrl.getHostname() + "does not support random reads");
            }
        } catch (VlException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.NoSuchPaddingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.DesEncrypter;
import nl.uva.cs.lobcder.util.SpeedLogger;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.*;
import nl.uva.vlet.vfs.cloud.CloudFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 * A test PDRI to implement the delete get/set data methods with the VRS API
 *
 * @author S. koulouzis
 */
@Log
public class VPDRI implements PDRI {

    static {
        try {
            InitGlobalVFS();
        } catch (Exception ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);
        GlobalConfig.setUserHomeLocation(new URL("file:///" + System.getProperty("user.home")));

        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);

        VRS.getRegistry().addVRSDriverClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class);
        Global.init();
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
    private static final Map<String, GridProxy> proxyCache = new HashMap<>();

    VPDRI(String fileName, Long storageSiteId, String resourceUrl, String username, String password, boolean encrypt, BigInteger keyInt, boolean doChunkUpload) throws IOException {
        try {
            this.fileName = fileName;
            this.resourceUrl = resourceUrl;
            String encoded = VRL.encode(fileName);
            vrl = new VRL(resourceUrl).appendPath(baseDir).append(encoded);
//            vrl = new VRL(resourceUrl).appendPath(baseDir).append(URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
            this.storageSiteId = storageSiteId;
            this.username = username;
            this.password = password;
            this.encrypt = encrypt;
            this.keyInt = keyInt;
            this.doChunked = doChunkUpload;
//            this.resourceUrl = resourceUrl;
            VPDRI.log.log(Level.FINE, "fileName: {0}, storageSiteId: {1}, username: {2}, password: {3}, VRL: {4}", new Object[]{fileName, storageSiteId, username, password, vrl});
            initVFS();
        } catch (VlException | MalformedURLException ex) {
            throw new IOException(ex);
        }
    }

    private void initVFS() throws VlException, MalformedURLException {
        this.vfsClient = new VFSClient();
        VRSContext context = this.vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url
        ServerInfo info = context.getServerInfoFor(vrl, true);
        String authScheme = info.getAuthScheme();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH)) {
            GridProxy gridProxy = proxyCache.get(password);
            if (gridProxy == null) {
                String proxyFile = "/tmp/myProxy";

                context.setProperty("grid.proxy.location", proxyFile);
                // Default to $HOME/.globus
                context.setProperty("grid.certificate.location", Global.getUserHome() + "/.globus");
                String vo = username;
                context.setProperty("grid.proxy.voName", vo);
                gridProxy = context.getGridProxy();

                if (gridProxy.isValid() == false) {
                    gridProxy.setEnableVOMS(true);
                    gridProxy.setDefaultVOName(vo);
                    gridProxy.createWithPassword(password);
                    if (gridProxy.isValid() == false) {
                        throw new VlException("Created Proxy is not Valid!");
                    }
                    proxyCache.put(password, gridProxy);
                }
            }


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
            vfsClient.openLocation(vrl).delete();
        } catch (VlException ex) {
            //Maybe it's from assimilation. We must remove the baseDir
            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("Couldn open location. Get NULL object for location")) {
                try {
                    //                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
//                    String encoded = VRL.encode(fileName);
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
                    vfsClient.openLocation(assimilationVRL).delete();

                } catch (VRLSyntaxException ex1) {
                    Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (VlException ex1) {
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
        try {
            if (vfsClient == null) {
                reconnect();
            }
            file = (VFile) vfsClient.openLocation(vrl);
            in = file.getInputStream();
//            in = ((VFile) vfsClient.openLocation(vrl)).getInputStream();
        } catch (Exception ex) {
            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("Couldn open location. Get NULL object for location:")) {
                try {
//                    VRL assimilationVRL = new VRL(resourceUrl).append(URLEncoder.encode(fileName, "UTF-8"));
                    VRL assimilationVRL = new VRL(resourceUrl).append(fileName);
                    in = ((VFile) vfsClient.openLocation(assimilationVRL)).getInputStream();
                    sleeTime = 5;
                } catch (VRLSyntaxException ex1) {
                    throw new IOException(ex1);
                } catch (VlException ex1) {
                    if (reconnectAttemts < Constants.RECONNECT_NTRY) {
                        try {
                            sleeTime = sleeTime + 5;
                            Thread.sleep(sleeTime);
                            reconnect();
                            getData();
                        } catch (InterruptedException ex2) {
                            throw new IOException(ex1);
                        }
                    } else {
                        throw new IOException(ex1);
                    }
                }
            } else if (reconnectAttemts < Constants.RECONNECT_NTRY) {
                try {
                    sleeTime = sleeTime + 5;
                    Thread.sleep(sleeTime);
                    reconnect();
                    getData();
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
        VPDRI.log.log(Level.FINE, "putData:");
//        VFile tmpFile = null;
        try {
            //            upload(in);
            VRL parentVrl = vrl.getParent();
            VDir remoteDir = vfsClient.mkdirs(parentVrl, true);
            vfsClient.createFile(vrl, true);
            out = vfsClient.getFile(vrl).getOutputStream();
            if (!getEncrypted()) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, out);
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
        VPDRI.log.log(Level.FINE, "getHostName: " + InetAddress.getLocalHost().getHostName());
        if (vrl.getScheme().equals("file")
                || StringUtil.isEmpty(vrl.getHostname())
                || vrl.getHostname().equals("localhost")
                || vrl.getHostname().equals("127.0.0.1")) {
            return InetAddress.getLocalHost().getHostName();
        } else {
            return vrl.getHostname();
        }
    }

    private Runnable getAsyncDelete(final VFSClient vfsClient, final VRL vrl) {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    vfsClient.openLocation(vrl).delete();
                } catch (VlException ex) {
                    Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
    }

    private Runnable getAsyncPutData(final VFSClient vfsClient, final InputStream in) {
        return new Runnable() {

            @Override
            public void run() {
            }
        };
    }

    private void fastCopy(ReadableByteChannel src, WritableByteChannel dest) throws IOException {
        VPDRI.log.log(Level.FINE, "fastCopy:");
//        OperatingSystemMXBean osMBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//        int size = (int) (osMBean.getFreePhysicalMemorySize() / 2000);
//        debug("\talloocated size: "+size);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.BUF_SIZE);
        int len;
        try {
            while ((len = src.read(buffer)) != -1) {
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
        } finally {
            src.close();
            dest.close();
        }
    }

    private void channelCopy(OutputStream out, InputStream in) throws IOException {
        FileChannel target = ((FileOutputStream) out).getChannel();
        FileChannel source = ((FileInputStream) in).getChannel();
        source.transferTo(0, source.size(), target);
        source.close();
        target.close();
    }

    @Override
    public long getLength() throws IOException {
        try {
            if (vfsClient == null) {
                reconnect();
            }
            return vfsClient.getFile(vrl).getLength();
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
        vfsClient.close();
        vfsClient.dispose();
//        VRS.exit();
        try {
            initVFS();
        } catch (VlException | MalformedURLException ex1) {
            throw new IOException(ex1);
        }
    }

    @Override
    public Long getChecksum() throws IOException {
        try {
            VFile physicalFile = vfsClient.getFile(vrl);
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
        CloudFile thisFile = (CloudFile) vfsClient.createFile(vrl, true);
        VFile sourceFile = vfsClient.openFile(new VRL(source.getURI()));
        thisFile.uploadFrom(sourceFile);
    }

    @Override
    public void replicate(PDRI source) throws IOException {
        try {
            VRL sourceVRL = new VRL(source.getURI());
            String sourceScheme = sourceVRL.getScheme();
            String desteScheme = vrl.getScheme();
            VPDRI.log.log(Level.FINE, "Start replicating {0} to {1}", new Object[]{source.getURI(), getURI()});
            double start = System.currentTimeMillis();
            if (desteScheme.equals("swift") && sourceScheme.equals("file")) {
                upload(source);
            } else {
                VDir remoteDir = vfsClient.mkdirs(vrl.getParent(), true);
                VFile destFile = vfsClient.createFile(vrl, true);
                VFile sourceFile = vfsClient.openFile(sourceVRL);
                vfsClient.copy(sourceFile, destFile);
            }
//            putData(source.getData());
            double elapsed = System.currentTimeMillis() - start;
            double speed = ((source.getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
            String msg = "Source: " + source.getHost() + " Destination: " + getHost() + " Replication_Speed: " + speed + " Kbites/sec Repl_Size: " + (getLength()) + " bytes";
            VPDRI.log.log(Level.INFO, msg);
            SpeedLogger.logSpeed(msg);


            getAsyncDelete(vfsClient, vrl).run();

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
}

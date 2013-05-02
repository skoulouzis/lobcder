/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.*;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import lombok.Getter;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.DesEncrypter;

/**
 * A test PDRI to implement the delete get/set data methods with the VRS API
 *
 * @author S. koulouzis
 */
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
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(VPDRI.class);
    private BigInteger keyInt;
    private boolean encrypt;

    VPDRI(String fileName, Long storageSiteId, String resourceUrl, String username, String password, boolean encrypt, BigInteger keyInt) throws IOException {
        try {
            this.fileName = fileName;
            vrl = new VRL(resourceUrl).appendPath(baseDir).append(URLEncoder.encode(fileName, "UTF-8"));
            //Encode:
//            String strURI = vrl.toURI().toASCIIString();
//            vrl = new VRL(strURI);
            this.storageSiteId = storageSiteId;
            this.username = username;
            this.password = password;
            this.encrypt = encrypt;
            this.keyInt = keyInt;
//            this.resourceUrl = resourceUrl;
            log.debug("fileName: " + fileName + ", storageSiteId: " + storageSiteId + ", username: " + username + ", password: " + password + ", VRL: " + vrl);
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
            //Use the username and password to get access to MyProxy 
            GridProxy proxy = new GridProxy(context);
            String pr = context.getProxyAsString();
            context.setGridProxy(proxy);
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
        info.store();
    }

    @Override
    public void delete() throws IOException {
        try {
            vfsClient.openLocation(vrl).delete();
        } catch (VlException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
        //        //it's void so do it asynchronously
        //        Runnable asyncDel = getAsyncDelete(this.vfsClient, vrl);
        //        asyncDel.run();
    }

    @Override
    public InputStream getData() throws IOException {
        InputStream in = null;
        try {
            in = ((VFile) this.vfsClient.openLocation(vrl)).getInputStream();
        } catch (VlException ex) {
            throw new IOException(ex);
        }
        return in;
    }

    @Override
    public void putData(InputStream in) throws IOException {
        OutputStream out = null;
        debug("putData:");
//        VFile tmpFile = null;
        try {
            //            upload(in);
            VDir remoteDir = vfsClient.mkdirs(vrl.getParent(), true);
            vfsClient.createFile(vrl, true);
            out = vfsClient.getFile(vrl).getOutputStream();
            if (!getEncrypted()) {
                CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((2 * 1024 * 1024), in, out);
                cBuff.startTransfer(new Long(-1));
            } else {
                DesEncrypter encrypter = new DesEncrypter(getKeyInt());
                encrypter.encrypt(in, out);
            }
            reconnectAttemts = 0;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new IOException(ex);
        } catch (VlException ex) {
            if (ex.getMessage() != null) {
                debug("\tVlException " + ex.getMessage());
            }
            if (reconnectAttemts <= 2) {
                debug("\treconnectAttemts " + reconnectAttemts);
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
//            if (tmpFile != null) {
//                try {
//                    tmpFile.delete();
//                } catch (VlException ex) {
//                    throw new IOException(ex);
//                }
//            }
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
        debug("getHostName: " + InetAddress.getLocalHost().getHostName());
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
        debug("fastCopy:");
//        OperatingSystemMXBean osMBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//        int size = (int) (osMBean.getFreePhysicalMemorySize() / 2000);
//        debug("\talloocated size: "+size);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
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
            return vfsClient.getFile(vrl).getLength();
        } catch (VlException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void reconnect() throws IOException {
        reconnectAttemts++;
        vfsClient.close();
        vfsClient.dispose();
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
        }
        return null;
    }

    private void debug(String msg) {
        if (debug) {
//            System.err.println(this.getClass().getName() + ": " + msg);
            log.debug(msg);
        }
    }

    private void upload(InputStream in) throws VlException, InterruptedException {
        VDir remoteDir = vfsClient.mkdirs(vrl.getParent(), true);
        VRL tmpVRL = new VRL("file:///" + System.getProperty("java.io.tmpdir"));
        VRL tmpFileVRL = tmpVRL.append(fileName);
        VFile tmpFile = vfsClient.createFile(tmpFileVRL, true);
        OutputStream out = tmpFile.getOutputStream();
        CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((1 * 1024 * 1024), in, out);
        cBuff.startTransfer(new Long(-1));
        VFSTransfer trans = vfsClient.asyncCopy(tmpFile, remoteDir);
        int time = 100;
        while (!trans.isDone()) {
            debug("trans.getProgress(): " + trans.getProgress());
            Thread.sleep(time);
            time = time * 2;
        }
    }

    @Override
    public void replicate(PDRI source) throws IOException {
        putData(source.getData());
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

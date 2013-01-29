/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.sun.management.OperatingSystemMXBean;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VChecksum;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

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
    private final String fileURI;
    private int reconnectAttemts = 0;
    private final static boolean debug = true;

    VPDRI(String fileURI, Long storageSiteId, String resourceUrl, String username, String password) throws IOException {
        try {
            this.fileURI = fileURI;
            vrl = new VRL(resourceUrl).appendPath(baseDir).append(fileURI);
            this.storageSiteId = storageSiteId;
            this.username = username;
            this.password = password;
//            this.resourceUrl = resourceUrl;
            initVFS();
        } catch (VlException ex) {
            throw new IOException(ex);
        } catch (MalformedURLException ex) {
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
            GridProxy proxy = null;
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
        //it's void so do it asynchronously
        Runnable asyncDel = getAsyncDelete(this.vfsClient, vrl);
        asyncDel.run();
    }

    @Override
    public InputStream getData() throws IOException {
        try {
            return ((VFile) this.vfsClient.openLocation(vrl)).getInputStream();
        } catch (VlException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void putData(InputStream in) throws IOException {
        OutputStream out = null;
        debug("putData:");
        try {
            vfsClient.mkdirs(vrl.getParent(), true);
            vfsClient.createFile(vrl, true);
            out = vfsClient.getFile(vrl).getOutputStream();

//            OperatingSystemMXBean osMBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//            int size = (int) (osMBean.getFreePhysicalMemorySize() / 1000);
//            debug("\tAlocated buff size: "+size);
            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((20*1024*1024), in, out);
            cBuff.startTransfer(new Long(-1));
            
            reconnectAttemts = 0;
//            final ReadableByteChannel inputChannel = Channels.newChannel(in);
//            final WritableByteChannel outputChannel = Channels.newChannel(out);
//            fastCopy(inputChannel, outputChannel);
        } catch (VlException ex) {
            debug("\tVlException "+ex.getMessage());
            if (reconnectAttemts <= 2) {
                debug("\treconnectAttemts "+reconnectAttemts);
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
    public String getURI() {
        return this.fileURI;
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
        final ByteBuffer buffer = ByteBuffer.allocateDirect(1024*10);
        int len;
        try {
            while ((len = src.read(buffer)) != -1) {
//            System.err.println("Read size: " + len);
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
            }
//        System.err.println("--------------");
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
        } catch (VlException ex1) {
            throw new IOException(ex1);
        } catch (MalformedURLException ex1) {
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
            System.err.println(this.getClass().getName() + ": " + msg);
        }
    }
}

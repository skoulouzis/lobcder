/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
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
            Logger.getLogger(StorageSite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(StorageSite.class.getName()).log(Level.SEVERE, null, ex);
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
        try {
            vfsClient.mkdirs(vrl.getParent(), true);
            vfsClient.createFile(vrl, true);
            
            out = vfsClient.getFile(vrl).getOutputStream();
            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer(Constants.BUF_SIZE, in, out);
            cBuff.startTransfer(new Long(-1));
//            final ReadableByteChannel inputChannel = Channels.newChannel(in);
//            final WritableByteChannel outputChannel = Channels.newChannel(out);
//            fastCopy(inputChannel, outputChannel);
        } catch (VlException ex) {
            throw new IOException(ex);
//            if (ex instanceof ResourceNotFoundException || ex.getMessage().contains("not found") || ex.getMessage().contains("Couldn open location") || ex.getMessage().contains("not found in container")) {
//                try {
//
//                } catch (VlException ex1) {
//                    throw new IOException(ex1);
//                }
//            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
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
    public String getURL() {
        return this.fileURI;
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
        final ByteBuffer buffer = ByteBuffer.allocateDirect(Constants.BUF_SIZE);
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
}

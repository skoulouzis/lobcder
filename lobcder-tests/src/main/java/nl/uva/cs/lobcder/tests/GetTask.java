/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author skoulouz
 */
public class GetTask implements Runnable {

    private VFSClient cl;
    private int reconnectAttemts;

    static {
        try {
            InitGlobalVFS();
        } catch (Exception ex) {
        }
    }

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
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
    private final String vrl;
    private final String username;
    private final String password;
    int sleeTime = 5;

    public GetTask(String vrl, String username, String password) throws VlException {
        cl = getVFSClient(vrl, username, password);
        this.vrl = vrl;
        this.username = username;
        this.password = password;
    }

    private VFSClient getVFSClient(String vrl, String username, String password) throws VlException {
        VFSClient vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url
        ServerInfo info = context.getServerInfoFor(new VRL(vrl), true);
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

        info.setAttribute("sshKnownHostsFile", System.getProperty("user.home") + "/.ssh/known_hosts");
        info.store();
        return vfsClient;
    }

    @Override
    public void run() {
        double start = System.currentTimeMillis();
        FileOutputStream out = null;
        File f = null;
        try {
            f = new File("/tmp/deleteme" + this.hashCode());
//                f.deleteOnExit();
            out = new FileOutputStream(f);
            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((512 *1024), cl.getFile(vrl).getInputStream(), out);
            cBuff.startTransfer((long) -1);
            double elapsed = System.currentTimeMillis() - start;
            System.err.println("Speed: " + (f.length() / elapsed));
            sleeTime = 5;
        } catch (Exception ex) {
            try {
                //            
                if (reconnectAttemts < 10) {
                    sleeTime = sleeTime + 20;
                    System.err.println("Reconnecting: " + reconnectAttemts + " sleep: " + sleeTime);
                    Thread.sleep(sleeTime);
                    reconnect();
                    run();
                } else {
//                    Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (InterruptedException ex1) {
                Logger.getLogger(GetTask.class.getName()).log(Level.SEVERE, null, ex1);
//          
            } catch (VlException ex1) {
                Logger.getLogger(GetTask.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void reconnect() throws VlException {
        reconnectAttemts++;
        cl.close();
        cl.dispose();
//        VRS.exit();
        cl = getVFSClient(vrl, username, password);
    }
}

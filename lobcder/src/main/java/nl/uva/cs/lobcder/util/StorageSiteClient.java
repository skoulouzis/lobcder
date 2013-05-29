/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author S. Koulouzis
 */
public class StorageSiteClient {

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
        GlobalConfig.setSystemProperty(GlobalConfig.PROP_BDII_HOSTNAME, "bdii2.grid.sara.nl:2170");
        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);
        VRS.getRegistry().addVRSDriverClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class);
        Global.init();
    }
    private VFSClient vfsClient;
    private final String username;
    private final String password;
    private final VRL vrl;
    private static final Map<String, GridProxy> proxyCache = new HashMap<>();

    public StorageSiteClient(String username, String password, String resourceUrl) throws VlException, MalformedURLException, Exception {
        this.username = username;
        this.password = password;
        this.vrl = new VRL(resourceUrl);
        initVFS();
    }

    private void initVFS() throws VlException, MalformedURLException, Exception {
        this.vfsClient = new VFSClient();
        VRSContext context = this.vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url
        ServerInfo info = context.getServerInfoFor(vrl, true);
        String authScheme = info.getAuthScheme();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH)) {

            String proxyFile = "/tmp/myProxy";
            System.out.println("Will create grid proxy at:" + proxyFile);

            context.setProperty("grid.proxy.location", proxyFile);
            // Default to $HOME/.globus
            context.setProperty("grid.certificate.location", Global.getUserHome() + "/.globus");
            String vo = username;
            context.setProperty("grid.proxy.voName", vo);

            GridProxy gridProxy = context.getGridProxy();

            if (gridProxy.isValid() == false) {
                gridProxy.setEnableVOMS(true);
                gridProxy.setDefaultVOName(vo);
                // throw new Exception("Invalid Grid Proxy, please create first");
//                String pwd = askPassphrase("Please enter passphrase.");
                System.out.println("--- Creating proxy ---");
                gridProxy.createWithPassword("pass@Amstel");
                if (gridProxy.isValid() == false) {
                    throw new Exception("Created Proxy is not Valid!");
                }
            }

            System.out.println("--- Valid Grid Proxy ---");
            System.out.println(" - proxy filename =" + gridProxy.getProxyFilename());
            System.out.println(" - proxy timeleft =" + gridProxy.getTimeLeftString());
            System.out.println(" - proxy VOMS enabled =" + gridProxy.getEnableVOMS());
            System.out.println(" - proxy VO =" + gridProxy.getVOName());
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
        context.setProperty("chunk.upload", false);
//        info.setAttribute(new VAttribute("chunk.upload", true));
        info.store();
    }

    public VFSClient getStorageSiteClient() {
        return this.vfsClient;
    }
}

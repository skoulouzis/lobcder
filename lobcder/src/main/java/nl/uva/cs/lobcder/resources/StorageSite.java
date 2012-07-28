/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.annotations.*;
import javax.jdo.identity.StringIdentity;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesLoader;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.ResourceNotFoundException;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author S. Koulouzis
 */
@PersistenceCapable(detachable = "true")
public class StorageSite implements Serializable, IStorageSite {

    private static final long serialVersionUID = -2552461454620784560L;

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
        GlobalConfig.setUserHomeLocation(new URL("file:///"+System.getProperty("user.home")));

        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);

        VRS.getRegistry().addVRSDriverClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class);
        Global.init();
    }
    @PrimaryKey
//    @Persistent(valueStrategy= IdGeneratorStrategy.UUIDSTRING)
    @Persistent
    private String uid;
    @Persistent
    private String endpoint;
    @Join
    @Persistent(defaultFetchGroup = "true")
    @Element(types = String.class)
    @Order(column = "LOGICAL_PATHS")
    private Collection<String> logicalPaths;
    @Join
    @Persistent(defaultFetchGroup = "true")
    @Element(types = String.class)
    @Order(column = "VPH_USERNAMES")
    private Collection<String> vphUsernames;
    @Persistent
    @Column(name = "VPH_USERNAMES_CSV", jdbcType = "VARCHAR", length = 500)
    private String vphUsernamesCSV;
    @Persistent(defaultFetchGroup = "true")
    private Credential credentials;
    private Properties prop;
    private VRL vrl;
    private ServerInfo info;
    private VRSContext context;
    private VFSClient vfsClient;
    @NotPersistent
    public static String storagePrefix = "LOBCDER-REPLICA-vTEST";
    @NotPersistent
    private static final boolean debug = true;

    public StorageSite(String endpoint, Credential cred) throws Exception {
        try {
            if (endpoint == null) {
                throw new NullPointerException("Endpoint is null");
            }
            this.endpoint = endpoint;
            if (cred == null) {
                throw new NullPointerException("Credentials are null");
            }
            if (cred.getVPHUsernames() == null) {
                throw new NullPointerException("vph Username is null");
            }

            vphUsernames = Arrays.asList(cred.getVPHUsernames());

            vphUsernamesCSV = "";
            String prefix = "";
            for (String n : vphUsernames) {
                vphUsernamesCSV += prefix;
                prefix = ",";
                vphUsernamesCSV += n;
            }

            storagePrefix = PropertiesLoader.getLobcderProperties().getProperty(Constants.LOBCDER_STORAGE_PREFIX);

            vrl = new VRL(endpoint + "/" + storagePrefix);

            prop = new Properties();

            this.credentials = cred;
            uid = new StringIdentity(this.getClass(), java.util.UUID.randomUUID().toString()).getKey();//vrl.toString()+prop.getProperty(Constants.STORAGE_SITE_USERNAME);//vrl.toString();//vrl.toString();   // //
//            if (credentials.getStorageSiteUsername() != null) {
//                uid+=credentials.getStorageSiteUsername();
//            }
//            if (credentials.getStorageSitePassword() != null) {
//                uid+=credentials.getStorageSitePassword();
//            }
//            if (credentials.getStorageSiteGridProxy() != null && credentials.getStorageSiteGridProxy().getUserDN() != null) {
//                uid+=credentials.getStorageSiteGridProxy().getUserDN();
//            }
            initVFS();
        } catch (VlException ex) {
            throw new Exception(ex);
        }

        logicalPaths = new ArrayList<String>();
    }

    @Override
    public VFSNode getVNode(Path path) {
        try {
            return getVfsClient().openLocation(getVrl().append(path.toString()));
        } catch (VlException ex) {
            if (ex instanceof ResourceNotFoundException) {
                //wrong cache 
                while (logicalPaths.contains(path.toString())) {
                    logicalPaths.remove(path.toString());
                }
            }
        }
        return null;
    }

    @Override
    public VFSNode createVFSFile(Path path) throws VlException {
        try {
            getVfsClient().mkdirs(getVrl(), true);
        } catch (Exception ex) {
            throw new VlException(ex);
        }
        String[] parts = path.getParts();
        if (parts.length > 1) {
            String parent = path.getParent().toString();
            debug("mkdirs: " + getVrl().append(parent));
            getVfsClient().mkdirs(getVrl().append(parent));
        }

        VRL newVRL = getVrl().append(path.toString());
        VFile node = getVfsClient().createFile(newVRL, true);
        if (!logicalPaths.contains(path.toString())) {
            logicalPaths.add(path.toString());
        }
        return node;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public Collection<String> getVPHUsernames() {
        return this.vphUsernames;
    }

    private void initVFS() throws VlException, MalformedURLException {
        this.vfsClient = new VFSClient();

        context = this.vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url 
        info = context.getServerInfoFor(getVrl(), true);
        String authScheme = info.getAuthScheme();
        GridProxy proxy = credentials.getStorageSiteGridProxy();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH) && proxy != null) {
            context.setGridProxy(proxy);
        }

        if (StringUtil.equals(authScheme, ServerInfo.PASSWORD_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSPHRASE_AUTH)) {
            String username = credentials.getStorageSiteUsername();
            if (username == null) {
                throw new NullPointerException("Username is null!");
            }
            info.setUsername(username);
            String password = credentials.getStorageSitePassword();
            if (password == null) {
                throw new NullPointerException("password is null!");
            }
            info.setPassword(password);
        }

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);
        
//        if(getVrl().getScheme().equals(VRS.SFTP_SCHEME)){
        //patch for bug with ssh driver 
            info.setAttribute("sshKnownHostsFile", System.getProperty("user.home")+"/.ssh/known_hosts");
//        }
        
        info.store();

    }

    @Override
    public Collection<String> getLogicalPaths() {
        return logicalPaths;
    }

    private void debug(String msg) {
//        if (debug) {
//            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        }
    }

    @Override
    public boolean LDRIHasPhysicalData(Path ldri) throws VlException {
        if (!logicalPaths.contains(ldri.toString())) {
            VRL newVRL = getVrl().append(ldri.toString());
            boolean hasPhysicalData = getVfsClient().existsPath(newVRL);
            if (hasPhysicalData && !logicalPaths.contains(ldri.toString())) {
                logicalPaths.add(ldri.toString());
            }
            return hasPhysicalData;
        }
        return logicalPaths.contains(ldri.toString());
    }

    @Override
    public Credential getCredentials() {
        return this.credentials;
    }

//    @Override
//    public String getUID() {
//        return this.uid;
//    }
    @Override
    public void deleteVNode(Path permenantDRI) throws VlException {
        debug("Exists?: " + permenantDRI);
        VRL theVRL = getVrl().append(permenantDRI.toString());
        boolean exists = getVfsClient().existsPath(theVRL);
        if (exists) {
            VFSNode node = this.getVNode(permenantDRI);
            boolean deleted = false;
            if(node instanceof VDir){
                deleted = ((VDir)node).deleteDir(theVRL.getDirPath(), true);
            }else{
                deleted= node.delete();
            }
            if (node != null && deleted) {
                this.logicalPaths.remove(permenantDRI.toString());
            }
        }
    }

    /**
     * @return the vfsClient
     */
    @Override
    public VFSClient getVfsClient() throws VlException {
        if (vfsClient == null) {
            try {
                initVFS();
            } catch (MalformedURLException ex) {
                throw new nl.uva.vlet.exception.VRLSyntaxException(ex);
            }
        }
        return vfsClient;
    }

    /**
     * @param vfsClient the vfsClient to set
     */
    public void setVfsClient(VFSClient vfsClient) {
        this.vfsClient = vfsClient;
    }

    @Override
    public ServerInfo getInfo() {
        return this.info;
    }

    /**
     * @return the vrl
     */
    private VRL getVrl() throws VRLSyntaxException {
        if (vrl == null) {
            vrl = new VRL(endpoint + "/" + storagePrefix);
        }
        return vrl;
    }

    public void setLogicalPaths(Collection<String> logicalPaths) {
        this.logicalPaths = logicalPaths;
    }

    /**
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * @param uid the uid to set
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public void removeLogicalPath(Path pdrI) {
        this.logicalPaths.remove(pdrI.toString());
        //We have to remove children as well 
//        for (String s : logicalPaths) {
//            debug("Remove?? " + s);
//        }
    }

    @Override
    public String getVPHUsernamesCSV() {
        return vphUsernamesCSV;
    }
}

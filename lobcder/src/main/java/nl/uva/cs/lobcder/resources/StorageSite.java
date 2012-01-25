/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author S. Koulouzis
 */
@PersistenceCapable
public class StorageSite implements Serializable, IStorageSite {

    private static final long serialVersionUID = -2552461454620784560L;

    static {
        try {
            InitGlobalVFS();
        } catch (MalformedURLException ex) {
            Logger.getLogger(StorageSite.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VlException ex) {
            Logger.getLogger(StorageSite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void InitGlobalVFS() throws MalformedURLException, VlException {
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

        // user configuration 
        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);

        Global.init();
    }
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    private long id;
    private Properties prop;
    private VRL vrl;
    @Persistent
    private String endpoint;
    @Persistent
    private ArrayList<String> logicalPaths = new ArrayList<String>();
    private ServerInfo info;
    private VRSContext context;
    private VFSClient vfsClient;
    @Persistent
    private String vphUsername;
    private final Credential credentials;

    public StorageSite(String endpoint, Credential cred) throws Exception {
        try {
            this.endpoint = endpoint;
            vphUsername = cred.getVPHUsername();
            vrl = new VRL(endpoint);

            prop = new Properties();

            this.credentials = cred;

            initVFS();
        } catch (VlException ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public VFSNode getVNode(Path path) throws VlException {
        return vfsClient.openLocation(vrl.append(path.toString()));
    }

    @Override
    public VFSNode createVFSFile(Path path) throws VlException {
        String[] parts = path.getParts();
        if (parts.length > 1) {
            String parent = path.getParent().toString();
            debug("mkdirs: " + vrl.append(parent));
            vfsClient.mkdirs(vrl.append(parent));
        }

        VRL newVRL = vrl.append(path.toString());
        VFile node = vfsClient.createFile(newVRL, true);
        logicalPaths.add(path.toString());
        return node;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getVPHUsername() {
        return vphUsername;
    }

    private void initVFS() throws VlException, MalformedURLException {
        vfsClient = new VFSClient();
        context = vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url 
        info = context.getServerInfoFor(vrl, true);
        String authScheme = info.getAuthScheme();
        GridProxy proxy = credentials.getStorageSiteGridProxy();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH) && proxy != null) {
            context.setGridProxy(proxy);
        }

        if (StringUtil.equals(authScheme, ServerInfo.PASSWORD_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSPHRASE_AUTH)) {
            info.setUsername(credentials.getStorageSiteUsername());
            info.setPassword(credentials.getStorageSitePassword());
        }

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);

        info.store();
    }

    @Override
    public Collection<String> getLogicalPaths() {
        return logicalPaths;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
    }

    @Override
    public boolean LDRIHasPhysicalData(Path ldri) {
        return logicalPaths.contains(ldri.toString());
    }

    @Override
    public Credential getCredentials() {
        return this.credentials;
    }

    @Override
    public long getUID() {
        return this.id;
    }
}

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
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFS;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VNode;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author S. Koulouzis
 */
@PersistenceCapable
public class StorageSite implements Serializable, IStorageSite {

    private static final long serialVersionUID = -2552461454620784560L;
    private Properties prop;
    private final VRL vrl;
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

    VFSNode getVNode(Path path) throws VlException {
        if (logicalPaths.contains(path.toString())) {
            return vfsClient.openLocation(vrl.append(path.toPath()));
        } else {
            return null;
        }
    }

    VFSNode createVFSNode(Path path) throws VlException {
        VFile node = vfsClient.newFile(vrl.append(path.toPath()));
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
        GlobalConfig.setAllowUserInteraction(false);
        GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
//        GlobalConfig.setUsePersistantUserConfiguration(false);
        GlobalConfig.setInitURLStreamFactory(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:///tmp"));

        Global.init();


//        context = new VRSContext(false);
        context = VRSContext.getDefault();

//        context.setUserHomeLocation(new VRL("file:///tmp"));
//        context.setVirtualRoot(VFS.newVDir(context, new VRL("file:///tmp")));

        String userHome = context.getLocalUserHome();

        debug(">>>>>The user home: " + userHome);

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

        vfsClient = new VFSClient(context);
    }

    @Override
    public Collection<String> getLogicalPaths() {
        return logicalPaths;
    }

    private void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
    }
}

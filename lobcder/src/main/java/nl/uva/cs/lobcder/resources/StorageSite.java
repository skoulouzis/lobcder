/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
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
    private Properties prop;
    private final VRL vrl;
    @Persistent
    private String endpoint;
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


        } catch (VRLSyntaxException ex) {
            throw new URISyntaxException(endpoint, ex.getMessage());
        } catch (VlException ex) {
            throw new Exception(ex);
        }
    }

    VFSNode getVNode(Path path) throws VlException {
        return vfsClient.openLocation(vrl.append(path.toPath()));

    }

    VFSNode createVFSNode(Path path) throws VlException {
        return vfsClient.newFile(vrl.append(path.toPath()));

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
        GlobalConfig.setUsePersistantUserConfiguration(false);

        context = new VRSContext(false);

        info = context.getServerInfoFor(vrl, true);
        
        if (info.getAuthScheme().equals(ServerInfo.GSI_AUTH) && credentials.getStorageSiteGridProxy() != null) {
            context.setGridProxy(credentials.getStorageSiteGridProxy());
        }
        
        if (info.getAuthScheme().equals(ServerInfo.PASSWORD_AUTH)
                || info.getAuthScheme().equals(ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                || info.getAuthScheme().equals(ServerInfo.PASSPHRASE_AUTH)) {
            info.setUsername(credentials.getStorageSiteUsername());
            info.setPassword(credentials.getStorageSitePassword());
        }

        vfsClient = new VFSClient(context);
    }
}

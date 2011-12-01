/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.annotations.PersistenceCapable;
import nl.uva.vlet.exception.VRLSyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VNode;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author S. Koulouzis
 */
@PersistenceCapable
public class StorageSite implements Serializable, IStorageSite {

    private static final long serialVersionUID = -2552461454620784560L;
    private String endpoint;
    private Credential cred;
    private Properties prop;
    private final VRL vrl;
    private ServerInfo info;
    private final VRSContext context;

    public StorageSite(String endpoint, Credential cred) throws Exception {
        try {
            this.endpoint = endpoint;
            vrl = new VRL(endpoint);
            this.cred = cred;

            prop = new Properties();
            context = new VRSContext();

            if (cred.getGridProxy() != null) {
                context.setGridProxy(cred.getGridProxy());
            }
            info = context.getServerInfoFor(vrl, true);


            if (info.getAuthScheme().equals(ServerInfo.PASSWORD_AUTH)
                    || info.getAuthScheme().equals(ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                    || info.getAuthScheme().equals(ServerInfo.PASSPHRASE_AUTH)) {
                info.setUsername(cred.getUsername());
                info.setPassword(cred.getPassword());
            }

        } catch (VRLSyntaxException ex) {
            throw new URISyntaxException(endpoint, ex.getMessage());
        } catch (VlException ex) {
            throw new Exception(ex);
        }
    }

    VFSNode getVNode() throws VlException {
        VFSClient c = new VFSClient(context);
        return c.openLocation(vrl);
    }
}

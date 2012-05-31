package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.catalogue.RDMSDLCatalog;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;
import nl.uva.cs.lobcder.util.PropertiesLoader;

public class WebDataResourceFactory implements ResourceFactory {

//    private Logger log = LoggerFactory.getLogger(WebDataResourceFactory.class);
    public static final String REALM = "vph-share";
    private IDLCatalogue catalogue;
    private boolean debug = false;
    //Hardcoded for now. We need to find a way to get the username
    private String uname = "skoulouz";

    public WebDataResourceFactory() throws Exception {
        String confDir = nl.uva.cs.lobcder.util.Constants.LOBCDER_CONF_DIR;
        File propFile = new File(confDir + "/datanucleus.properties");
        if (!propFile.exists()) {
            throw new IOException("Properties file " + propFile.getAbsolutePath() + " is missing");
        }
        catalogue = new RDMSDLCatalog(propFile);
        initStorageSites();
    }

    @Override
    public Resource getResource(String host, String strPath) {

        Path ldri = Path.path(new File(strPath).getPath()).getStripFirst();
        try {
            //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
            debug("getResource:  strPath: " + strPath + " path: " + Path.path(strPath) + " ldri: " + ldri);
            debug("getResource:  host: " + host + " path: " + ldri);
            debug("getResource:  uname : " + this.uname);

            Collection<IStorageSite> sites;
            if (ldri.isRoot() || ldri.toString().equals("")) {
                LogicalData root = new LogicalData(ldri, Constants.LOGICAL_FOLDER);
                ArrayList<Integer> permArr = new ArrayList<Integer>();
                permArr.add(0);
                permArr.add(Permissions.OWNER_ROLE | Permissions.READWRITE);
                permArr.add(Permissions.REST_ROLE | Permissions.NOACCESS);
                permArr.add(Permissions.ROOT_ADMIN | Permissions.READWRITE);
                root.getMetadata().setPermissionArray(permArr);
                WebDataDirResource webRoot = new WebDataDirResource(catalogue, root);                
                return webRoot;
            }

            ILogicalData entry = catalogue.getResourceEntryByLDRI(ldri);
            if (entry == null) {
                debug("Didn't find " + ldri + ". returning null");
                return null;
            }

            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                return new WebDataDirResource(catalogue, entry);
            }
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                return new WebDataFileResource(catalogue, entry);
            }
            return new WebDataResource(catalogue, entry);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(WebDataResourceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }
    }

    private void initStorageSites() throws Exception {
//        String[] names = new String[]{"storage1.prop", "storage2.prop", "storage3.prop"};


        Properties[] storageSitesProps = PropertiesLoader.getStorageSitesProps();
        for (Properties p : storageSitesProps) {
            if (!catalogue.storageSiteExists(p)) {
                catalogue.registerStorageSite(p);
            }
        }
    }

    public void setUserName(String remoteUser) {
        this.uname = remoteUser;
    }
}

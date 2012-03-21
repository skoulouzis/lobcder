package nl.uva.cs.lobcder.webDav.resources;

import java.util.logging.Level;


import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;

public class WebDataResourceFactory implements ResourceFactory {

//    private Logger log = LoggerFactory.getLogger(WebDataResourceFactory.class);
    public static final String REALM = "vph-share";
    private IDLCatalogue catalogue;
    private boolean debug = true;
    //Hardcoded for now. We need to find a way to get the username
    private String uname = "uname1";

    public WebDataResourceFactory() throws Exception {
        catalogue = new SimpleDLCatalogue();
        initStorageSites();
    }

    @Override
    public Resource getResource(String host, String strPath) {

        Path ldri = Path.path(strPath).getStripFirst();
        Collection<IStorageSite> sites;
        LogicalData root;
        try {
            
            //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
            debug("getResource:  strPath: " + strPath + " path: " + Path.path(strPath) + " ldri: " + ldri);
            debug("getResource:  host: " + host + " path: " + ldri);

//            if (host == null && Path.path(strPath).toString().equals("")) {
//                debug(">>>>>>>>>>>>>>> Host null and path is empty");
//            }
            
            if (ldri.isRoot() || ldri.toString().equals("")) {
                root = new LogicalData(ldri);
                sites = root.getStorageSites();
                if (sites == null || sites.isEmpty()) {
                    sites = (ArrayList<IStorageSite>) catalogue.getSitesByUname(uname);
                    
                    if (sites == null || sites.isEmpty()) {
                        debug("\t StorageSites for " + ldri + " are empty!");
                        throw new IOException("StorageSites for " + ldri + " are empty!");
                    }
                    root.setStorageSites(sites);
                }
                return new WebDataDirResource(catalogue, root);
            }
            
            ILogicalData entry = catalogue.getResourceEntryByLDRI(ldri);
            if (entry == null) {
                debug("Didn't find " + ldri + ". returning null");
                return null;
            }

            sites = entry.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                sites = (ArrayList<IStorageSite>) catalogue.getSitesByUname(uname);
                if (sites == null || sites.isEmpty()) {
                    debug("\t StorageSites for " + ldri + " are empty!");
                    throw new IOException("StorageSites for " + ldri + " are empty!");
                }
                entry.setStorageSites(sites);
            }



            if (entry instanceof LogicalFolder) {
                return new WebDataDirResource(catalogue, entry);
            }
            if (entry instanceof LogicalFile) {
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
        String[] names = new String[]{"storage1.prop"};

        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;

        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            if (!catalogue.storageSiteExists(prop)) {
                catalogue.registerStorageSite(prop);
            }
        }
    }

    private static Properties getCloudProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        if(!f.exists()){
            throw  new FileNotFoundException("Configuration file "+f.getAbsolutePath()+" not found");
        }
        properties.load(new FileInputStream(f));
        return properties;
    }
}

package nl.uva.cs.lobcder.webDav.resources;

import java.util.Collection;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.exceptions.BadRequestException;
import java.io.IOException;
import java.net.URISyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.LogicalFile;
import nl.uva.cs.lobcder.resources.LogicalFolder;

public class WebDataResourceFactory implements ResourceFactory {

    private Logger log = LoggerFactory.getLogger(WebDataResourceFactory.class);
    public static final String REALM = "vph-share";
    private IDLCatalogue catalogue;
    private boolean debug = true;

    public WebDataResourceFactory() throws URISyntaxException, VlException, IOException {
        catalogue = new SimpleDLCatalogue();
    }

    @Override
    public Resource getResource(String host, String strPath) {

        Path ldri = Path.path(strPath).getStripFirst();
        try {
            //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
            debug("getResource:  strPath: " + strPath + " path: " + Path.path(strPath));
            debug("getResource:  host: " + host + " path: " + ldri);
            
//            if (host == null && Path.path(strPath).toString().equals("")) {
//                debug(">>>>>>>>>>>>>>> Host null and path is empty");
//            }

            if (ldri.isRoot() || ldri.toString().equals("")) {
                return new WebDataDirResource(catalogue, new LogicalData(ldri));
            }

            ILogicalData entry = catalogue.getResourceEntryByLDRI(ldri);
            if (entry == null) {
                debug("Didn't find " + ldri + ". returning null");
                return null;
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
}

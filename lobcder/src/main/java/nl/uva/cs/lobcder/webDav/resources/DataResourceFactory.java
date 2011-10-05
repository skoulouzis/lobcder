package nl.uva.cs.lobcder.webDav.resources;

import java.util.Collection;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import nl.uva.vlet.exception.VlException;
import nl.uva.cs.lobcder.catalogue.IDRCatalogue;
import nl.uva.cs.lobcder.catalogue.SimpleDRCatalogue;
import nl.uva.cs.lobcder.resources.IDataResourceEntry;
import nl.uva.cs.lobcder.resources.DataResourceEntry;
import nl.uva.cs.lobcder.resources.ResourceFileEntry;
import nl.uva.cs.lobcder.resources.ResourceFolderEntry;

public class DataResourceFactory implements ResourceFactory {

    private Logger log = LoggerFactory.getLogger(DataResourceFactory.class);
    public static final String REALM = "vph-share";
    private IDRCatalogue catalogue;
    private boolean debug = true;

    public DataResourceFactory() throws URISyntaxException, VlException, IOException {
        catalogue = new SimpleDRCatalogue();
    }

    @Override
    public Resource getResource(String host, String strPath) {
        IDataResourceEntry rootEntry;
        Collection<IDataResourceEntry> topLevelEntries;

        try {
            //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
            Path ldri = Path.path(strPath).getStripFirst();
            debug("getResource:  host: " + host + " path: " + ldri);
//            if (ldri.isRoot() || ldri.toString().equals("")) {
//                rootEntry = catalogue.getResourceEntryByLDRI(ldri);
//                if (rootEntry == null) {
//                    rootEntry = new DataResourceEntry(ldri);
//                    topLevelEntries = catalogue.getTopLevelResourceEntries();
//                    for (IDataResourceEntry e : topLevelEntries) {
//                        debug("Root elements: "+e.getUID()+" "+e.getLDRI());
//                        rootEntry.addChild(e.getLDRI());
//                    }
//
//                }
//            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(DataResourceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new DummyResource();
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }
    }

    private Resource getResource(Path path) throws Exception {
        DataResourceEntry entry = (DataResourceEntry) catalogue.getResourceEntryByLDRI(path);
        if (entry == null) {
            throw new NullPointerException("Path " + path + " doesn't exist");
        }

        if (entry instanceof ResourceFolderEntry) {
            return new DataDirResource(entry);
        }
        if (entry instanceof ResourceFileEntry) {
            return new DataFileResource(entry);
        }
        debug("Unknown Type: " + entry.getLDRI());
        return new DataResource(entry);
    }
}

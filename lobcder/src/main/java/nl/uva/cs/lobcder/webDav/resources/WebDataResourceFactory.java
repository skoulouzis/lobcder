package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.resource.Resource;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

@Log
public class WebDataResourceFactory implements ResourceFactory {

    @Setter
    private JDBCatalogue catalogue;
    @Setter
    private AuthI auth1;
    @Setter
    private AuthI auth2;

    @Override
    public Resource getResource(String host, String strPath) {

        //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
        Path ldri = Path.path(strPath).getStripFirst().getStripFirst();
//        if (strPath.equals("/")) {
//            ldri = Path.root;
//        } else {
//            ldri = Path.path(strPath);
//        }

        try (Connection cn = catalogue.getConnection()) {
            WebDataResourceFactory.log.fine("getResource:  strPath: " + strPath + " path: " + Path.path(strPath) + " ldri: " + ldri + "\n"
                    + "\tgetResource:  host: " + host + " path: " + ldri);

            LogicalData entry = catalogue.getLogicalDataByPath(ldri, cn);
            if (entry == null) {
                return null;
            }

            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                return new WebDataDirResource(entry, ldri, catalogue, auth1, auth2);
            }
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                return new WebDataFileResource(entry, ldri, catalogue, auth1, auth2);
            }
            return null;
        } catch (SQLException ex) {
            WebDataResourceFactory.log.log(Level.SEVERE, null, 1);
            throw new RuntimeException(ex);
        }
    }
}

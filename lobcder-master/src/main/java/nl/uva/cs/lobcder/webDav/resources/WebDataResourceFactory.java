package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.resource.Resource;
import java.io.UnsupportedEncodingException;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringEscapeUtils;

@Log
public class WebDataResourceFactory implements ResourceFactory {

    @Setter
    private JDBCatalogue catalogue;
    @Setter
    private List<AuthI> authList;
//    @Setter
//    private AuthI auth2;
//    @Setter
//    @Setter
//    private AuthI auth3;
    private int attempts = 0;

    @Override
    public Resource getResource(String host, String strPath) {

        if (strPath.equals("/login.html")) {
            return null;
        }

        Path ldri = Path.path(strPath);
        String first;
        do {
            first = ldri.getFirst();
            ldri = ldri.getStripFirst();
        } while (!first.equals("dav"));
        String escaedPath = StringEscapeUtils.escapeSql(strPath);
        strPath = StringEscapeUtils.escapeHtml(escaedPath);
//        try (Connection cn = catalogue.getConnection()) {
        try {
            WebDataResourceFactory.log.log(Level.FINE, "getResource:  strPath: {0} path: {1} ldri: {2}" + "\n" + "\tgetResource:  host: {3} path: {4}", new Object[]{strPath, Path.path(strPath), ldri, host, ldri});

//            LogicalData entry = catalogue.getLogicalDataByPath(ldri, cn);
            LogicalData entry = catalogue.getLogicalDataByPath(ldri);
            if (entry == null) {
                return null;
            }
            catalogue.updateAccessTime(entry.getUid());


            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                return new WebDataDirResource(entry, ldri, catalogue, authList);
            }
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                return new WebDataFileResource(entry, ldri, catalogue, authList);
            }
            attempts = 0;
//            return null;
        } catch (SQLException ex) {
            if (attempts <= Constants.RECONNECT_NTRY) {
                attempts++;
                getResource(host, strPath);
            } else {
                WebDataResourceFactory.log.log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataResourceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}

package nl.uva.cs.lobcder.util;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.beans.PdriBean;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.resources.PDRIFactory;

import java.util.logging.Level;

/**
 * Created by dvasunin on 13.03.15.
 */
@Log
public class DeleteHelper {
    static public void delete(PdriBean pdriBean) throws Exception {
        try {
            PDRI pdri = PDRIFactory.getFactory().createInstance(pdriBean);
            if (pdri.exists(pdri.getFileName())) {
                pdri.delete();
            }
        } catch (Exception e) {
            if (e.getMessage().contains("No route to host") || e.getMessage().contains("Moved Permanently") || e instanceof java.net.UnknownHostException) {
                log.log(Level.WARNING, null, e);
            } else {
                throw e;
            }
        }
    }
}

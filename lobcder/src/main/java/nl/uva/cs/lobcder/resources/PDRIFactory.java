/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.IOException;
import javax.annotation.Nonnull;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;

/**
 *
 * @author dvasunin
 */
public class PDRIFactory {

    static private PDRIFactory factory = new PDRIFactory();

    static public PDRIFactory getFactory() {
        return factory;
    }

    public PDRI createInstance(PDRIDescr descr, boolean isCahce) throws IOException {
        if (isCahce && descr.getResourceUrl().contains("swift")) {
            return new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword(), descr.getEncrypt(), descr.getKey(), true);
        } else {
            return new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword(), descr.getEncrypt(), descr.getKey(), false);
        }
    }
}

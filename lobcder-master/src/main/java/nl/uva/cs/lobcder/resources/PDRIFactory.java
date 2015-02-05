/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.IOException;

/**
 *
 * @author dvasunin
 */
public class PDRIFactory {

    static private PDRIFactory factory = new PDRIFactory();

    static public PDRIFactory getFactory() {
        return factory;
    }

    public PDRI createInstance(PDRIDescr descr) throws IOException {
        return new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword(), descr.getEncrypt(), descr.getKey(), false);
    }

    public PDRI createInstance(PDRIDescr descr, boolean isCahce) throws IOException {
        VPDRI pdri;
        if (isCahce && descr.getResourceUrl().contains("swift")) {
            pdri = new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword(), descr.getEncrypt(), descr.getKey(), true);
        } else {
            pdri = new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword(), descr.getEncrypt(), descr.getKey(), false);
        }
        pdri.setIsCahce(isCahce);
        return pdri;
    }
}

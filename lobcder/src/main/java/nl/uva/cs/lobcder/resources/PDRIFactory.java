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
        return new VPDRI(descr.getName(), descr.getStorageSiteId(), descr.getResourceUrl(), descr.getUsername(), descr.getPassword());
    }
}

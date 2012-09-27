/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

/**
 *
 * @author dvasunin
 */
public class PDRIFactory {
    static private PDRIFactory factory = new PDRIFactory();
    
    static public PDRIFactory getFactory() {
        return factory;
    }
    
    public PDRI createInstance(Long storageSiteId, String url, String username, String password) {
        return new SimplePDRI(storageSiteId, url);
    }
    
}

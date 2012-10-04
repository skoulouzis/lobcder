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
    
    public PDRI createInstance(String url, Long storageSiteId, String resourceUrl, String username, String password) {
        if(resourceUrl.startsWith("file://")) {
            return new SimplePDRI(storageSiteId, url);
        } else 
            return null;
    }
    
}

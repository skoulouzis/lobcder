/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.bdii.BdiiService;
import nl.uva.vlet.util.bdii.StorageArea;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author skoulouz
 */
public class RegisterSRMSites {

    static {
        try {
            InitGlobalVFS();
        } catch (Exception ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static VFSClient vfsClient;
    private static ArrayList<StorageArea> srms;

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);
        GlobalConfig.setUserHomeLocation(new URL("file:///" + System.getProperty("user.home")));
        GlobalConfig.setSystemProperty(GlobalConfig.PROP_BDII_HOSTNAME, "bdii2.grid.sara.nl:2170");
        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);
        Global.init();
    }
    
    
    
      private static void initVFS() throws VlException, MalformedURLException, NamingException, Exception {
        vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();
                BdiiService bdii = context.getBdiiService();
        srms = bdii.getSRMv22SAsforVO("biomed");
//        
        debug("srms: "+context.getConfigManager().getBdiiHost());
        
        for(StorageArea inf : srms){
           debug("srms: "+inf.getVOStorageLocation());
        }        
        JDBCatalogue cat = new JDBCatalogue();
        String resourceURI = "";
        Credential credentials = new Credential();
        
        cat.registerStorageSite(resourceURI, credentials, -1, -1, -1, -1, null);
    }

    private static void debug(String msg) {
        System.err.println(RegisterSRMSites.class.getName()+": "+msg);
    }
    
    
    public static void main(String args[]){
        try {
            initVFS();
        } catch (Exception ex) {
            Logger.getLogger(RegisterSRMSites.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            VRS.exit();
        }
    }
}

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
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.bdii.ServiceInfo;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrs.ServerInfo;
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
        GlobalConfig.setSystemProperty(GlobalConfig.PROP_BDII_HOSTNAME, null);
        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);
        Global.init();
    }
    
    
    
      private static void initVFS() throws VlException, MalformedURLException {
        vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();
        debug("srms: "+context.getConfigManager().getBdiiHost());
        
        ArrayList<ServiceInfo> srms = VRS.createVRSClient(context).queryServiceInfo("biomed", ServiceInfo.ServiceInfoType.SRMV11);
        
        for(ServiceInfo inf : srms){
           debug("srms: "+inf.getHost());
        }
    }

    private static void debug(String msg) {
        System.err.println(RegisterSRMSites.class.getName()+": "+msg);
    }
    
    
    public static void main(String args[]){
        try {
            initVFS();
        } catch (VlException ex) {
            Logger.getLogger(RegisterSRMSites.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedURLException ex) {
            Logger.getLogger(RegisterSRMSites.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            VRS.exit();
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSClient;
import nl.uva.vlet.vrs.VRSContext;

/**
 * 
 * @author S. Koulouzis
 */
public class Config {

    public static final String TEST_BASE_URI = "file://"
            + System.getProperty("user.home")
            + "/workspace/lobcder/src/test/testData/";
//    public static final String DB_LOC_URI = TEST_BASE_URI + "/db/dbFile";

//     public static final String TEST_CONF_URI = TEST_BASE_URI + "/lobcder.conf";

    public static VRSClient initVRS() throws MalformedURLException {

        GlobalConfig.setHasUI(false);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setBaseLocation(new URL("file://dummy/path"));
        VRSContext cont = VRS.getDefaultVRSContext();

        return new VRSClient(cont);
    }

    private static void debug(String msg) {
        System.err.println(Config.class.getSimpleName()+": "+msg);
    }
}

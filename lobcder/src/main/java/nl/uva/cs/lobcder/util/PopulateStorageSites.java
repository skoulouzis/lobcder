/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import nl.uva.cs.lobcder.resources.StorageSiteManager;

/**
 *
 * @author S. Koulouzis
 */
public class PopulateStorageSites {

    public static void main(String args[]) throws FileNotFoundException, IOException, Exception {
        StorageSiteManager s;
        String[] names = new String[]{"storage2.prop", "storage3.prop"};

        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;
        
        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            String vphUname = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.VPH_USERNAME);
            s = new StorageSiteManager(vphUname);
            s.registerStorageSite(prop);
        }
    }

    private static Properties getCloudProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
    }
}

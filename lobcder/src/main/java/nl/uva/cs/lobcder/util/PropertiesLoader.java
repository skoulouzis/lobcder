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

/**
 *
 * @author skoulouz
 */
public class PropertiesLoader {

    private static final Object lock = new Object();

    public static Properties getLobcderProperties()
            throws FileNotFoundException, IOException {
        synchronized (lock) {
            Properties properties = new Properties();
            File f = new File(Constants.LOBCDER_CONF_DIR + "lobcder.prop");
            if (!f.exists()) {
                throw new FileNotFoundException("Configuration file " + f.getAbsolutePath() + " not found");
            }
            properties.load(new FileInputStream(f));
            return properties;
        }
    }

    public static Properties[] getStorageSitesProps() throws FileNotFoundException, IOException {
        String storageSitesFilesPaths = getLobcderProperties().getProperty(Constants.STORAGE_SITES_PROP_FILES);
        synchronized (lock) {
            String[] paths = storageSitesFilesPaths.split(",");
            Properties[] properties = new Properties[paths.length];
            for (int i = 0; i < paths.length; i++) {
                properties[i] = new Properties();
                File f = new File(paths[i]);
                if (!f.exists()) {
                    throw new FileNotFoundException("Configuration file " + f.getAbsolutePath() + " not found");
                }
                properties[i].load(new FileInputStream(f));
            }
            return properties;
        }
    }
}

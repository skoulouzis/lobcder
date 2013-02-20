/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.*;
import java.util.Properties;

/**
 *
 * @author skoulouz
 */
public class PropertiesLoader {
/*
    private static int numOfStorgeSites = -1;

    public static Properties getLobcderProperties()
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        File f = new File(Constants.LOBCDER_CONF_DIR + "lobcder.prop");
        if (!f.exists()) {
            throw new FileNotFoundException("Configuration file " + f.getAbsolutePath() + " not found");
        }
        properties.load(new FileInputStream(f));
        return properties;
    }

    public static Properties[] getStorageSitesProps() throws FileNotFoundException, IOException {
        String storageSitesFilesPaths = getLobcderProperties().getProperty(Constants.STORAGE_SITES_PROP_FILES);
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
        numOfStorgeSites = properties.length;
        return properties;
    }

    public static int getNumOfStorageSites() throws FileNotFoundException, IOException {
        if (numOfStorgeSites < 0) {
            numOfStorgeSites = getStorageSitesProps().length;
        }
        return numOfStorgeSites;
    }

    public static Properties getProperties(File file)
            throws FileNotFoundException, IOException {
        InputStream fis = null;
        try {
            Properties properties = new Properties();

            if (!file.exists()) {
                throw new FileNotFoundException("Configuration file " + file.getAbsolutePath() + " not found");
            }
            fis = new FileInputStream(file);
            properties.load(fis);
            return properties;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    */   
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author S. Koulouzis
 */
class Util {

    public static Properties getTestProperties(InputStream in)
            throws IOException {
        Properties properties = new Properties();
        properties.load(in);

        return properties;
    }
}
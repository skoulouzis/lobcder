/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author alogo
 */
public class TestSettings {

    public static final String TEST_FILE_NAME = "testFileName";
    public static final String TEST_DATA = "Tell me, O muse, of that ingenious hero who travelled "
            + "far and wide after he had sacked the famous town of Troy. "
            + "Many cities did he visit, and many were the nations with "
            + "whose manners and customs he was acquainted; moreover he "
            + "suffered much by sea while trying to save his own life "
            + "and bring his men safely home; but do what he might he "
            + "could not save his men, for they perished through their "
            + "own sheer folly in eating the cattle of the Sun-god "
            + "Hyperion; so the god prevented them from ever reaching "
            + "home.";
    
    public static final String TEST_TXT_FILE_NAME = "testTxtFileName.txt";

    public static Properties getTestProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
    }
}

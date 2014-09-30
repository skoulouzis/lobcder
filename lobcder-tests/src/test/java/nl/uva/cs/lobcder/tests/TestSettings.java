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
 * @author S. Koulouzis
 */
public class TestSettings {

    public static final String TEST_FILE_NAME1 = "testFileName1";
    public static final String TEST_FILE_NAME2 = "testFileName2";
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
    public static final String TEST_DATA_1KByte = "Persephone's nightmare  Where once herbs were grown in the fields with leaves and flowers full of fragrance Now there’re plants making concrete and steel and birds fall dead in melting furnace  Sleep away Persephone inside earth’s embrace never, never come up to our world again  The place where priests stood in devotion before the ritual of mysteries begins now, passing tourists throw their litters and they rush to see the new refinery  Sleep away Persephone inside earth’s embrace never, never come up to our world again  Once the sea was blue and crystal clear and flocks and herds carefree grazed in the fields now dusty tracks carry workers to the shipyards spreading noise and pollution all around  Sleep away Persephone Inside earth’s embrace Never, never come up to our world again Persephone's nightmare  Where once herbs were grown in the fields with leaves and flowers full of fragrance Now there’re plants making concrete and steel and birds fall dead in melting furnace  Sleep away Persephone inside earth’s";
    public static final String TEST_TXT_FILE_NAME = "testTxtFileName.txt";
    public static final String BACKEND_ENDPOINT = "backend.endpoint";
    public static final String BACKEND_USERNAME = "backend.username";
    public static final String BACKEND_PASSWORD = "backend.password";

    public static Properties getTestProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
    }
}

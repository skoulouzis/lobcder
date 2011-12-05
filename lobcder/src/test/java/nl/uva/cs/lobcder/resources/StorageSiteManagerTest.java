/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author skoulouz
 */
public class StorageSiteManagerTest {

    private static String[] names = new String[]{"storage2.prop", "storage3.prop"};
    private String vphUname;

    public StorageSiteManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSites method, of class StorageSiteManager.
     */
    @Test
    public void testGetSites() throws Exception {
        System.out.println("getSites");
        populateStorageSites();

        StorageSiteManager instance = new StorageSiteManager(vphUname);
        
        ArrayList result = instance.getSites();
        assertNotNull(result);
    }

    private void populateStorageSites() throws FileNotFoundException, IOException, Exception {


        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder"
                + File.separator + "etc" + File.separator;

        for (String name : names) {
            Properties prop = getCloudProperties(propBasePath + name);
            vphUname = prop.getProperty(nl.uva.cs.lobcder.webdav.Constants.Constants.VPH_USERNAME);
            StorageSiteManager instance = new StorageSiteManager(vphUname);
            instance.registerStorageSite(prop);
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

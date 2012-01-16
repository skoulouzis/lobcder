/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import java.util.Collection;
import java.util.Date;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.ContantsAndSettings;
import java.io.ByteArrayInputStream;
import com.bradmcevoy.http.Resource;
import nl.uva.cs.lobcder.catalogue.SimpleDLCatalogue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author alogo
 */
public class WebDataResourceFactoryTest {

    public WebDataResourceFactoryTest() {
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
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testGetResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";
        String strPath = "/";
        WebDataResourceFactory instance = new WebDataResourceFactory();
//        Resource expResult = null;
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, strPath);
        assertNotNull(result);

        ByteArrayInputStream bais = new ByteArrayInputStream(ContantsAndSettings.TEST_DATA.getBytes());
        result.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long(ContantsAndSettings.TEST_DATA.getBytes().length), "text/plain");

        result.delete();
    }

    /**
     * Test of getResource method, of class WebDataResourceFactory.
     */
    @Test
    public void testCreateGetAndDeleteResource() throws Exception {
        System.out.println("getResource");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ContantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        ByteArrayInputStream bais = new ByteArrayInputStream(ContantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long("DATA".getBytes().length), "text/plain");
        Long len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        String acceps = "text/html,text/*;q=0.9";
        String res = file.getContentType(acceps);
        String expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        Date date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        String name = file.getName();
        assertEquals(ContantsAndSettings.TEST_FILE_NAME, name);

        result.delete();

        instance = new WebDataResourceFactory();
        result = (WebDataDirResource) instance.getResource(host, ContantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);

        bais = new ByteArrayInputStream(ContantsAndSettings.TEST_DATA.getBytes());
        file = (WebDataFileResource) result.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long("DATA".getBytes().length), "text/plain");
        len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        acceps = "text/html,text/*;q=0.9";
        res = file.getContentType(acceps);
        expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        name = file.getName();
        assertEquals(ContantsAndSettings.TEST_FILE_NAME, name);

        result.delete();

    }

    @Test
    public void testCreateGetAndDeleteResource2() throws Exception {

        System.out.println("getResource");
        String host = "localhost:8080";

        WebDataResourceFactory instance = new WebDataResourceFactory();
        WebDataDirResource result = (WebDataDirResource) instance.getResource(host, ContantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);


        ByteArrayInputStream bais = new ByteArrayInputStream(ContantsAndSettings.TEST_DATA.getBytes());
        WebDataFileResource file = (WebDataFileResource) result.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long("DATA".getBytes().length), "text/plain");
        Long len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        String acceps = "text/html,text/*;q=0.9";
        String res = file.getContentType(acceps);
        String expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        Date date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        String name = file.getName();
        assertEquals(ContantsAndSettings.TEST_FILE_NAME, name);

        file.delete();

        SimpleDLCatalogue cat = new SimpleDLCatalogue();
        ILogicalData entry = cat.getResourceEntryByLDRI(file.getPath());
        assertNull(entry);

        result = (WebDataDirResource) instance.getResource(host, ContantsAndSettings.TEST_FOLDER_NAME);
        assertNotNull(result);


        file = (WebDataFileResource) result.createNew(ContantsAndSettings.TEST_FILE_NAME, bais, new Long("DATA".getBytes().length), "text/plain");
        len = file.getContentLength();
        assertEquals(len, new Long("DATA".getBytes().length));

        acceps = "text/html,text/*;q=0.9";
        res = file.getContentType(acceps);
        expResult = "text/*;q=0.9";
        assertEquals(expResult, res);

        date = file.getCreateDate();
        assertNotNull(date);
        date = file.getModifiedDate();
        assertNotNull(date);


        name = file.getName();
        assertEquals(ContantsAndSettings.TEST_FILE_NAME, name);


        result.delete();
        cat = new SimpleDLCatalogue();
        entry = cat.getResourceEntryByLDRI(result.getPath());
        assertNull(entry);

    }
}

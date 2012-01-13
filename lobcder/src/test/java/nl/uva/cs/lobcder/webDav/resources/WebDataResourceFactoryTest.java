/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import java.io.ByteArrayInputStream;
import com.bradmcevoy.http.Resource;
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
        
        ByteArrayInputStream bais = new ByteArrayInputStream("DATA".getBytes());
        result.createNew("newName", bais, new Long("DATA".getBytes().length), "text/plain");
    }
}

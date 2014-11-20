/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author S. Koulouzis
 */
public class MMTypeToolsTest {
    
    public MMTypeToolsTest() {
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
     * Test of bestMatch method, of class MMTypeTools.
     */
    @Test
    public void testBestMatch() {
        System.out.println("bestMatch");
        
        Collection<String> acceps = new ArrayList<String>();
        acceps.add("text/html");
        acceps.add("text/*;q=0.9");
        acceps.add("image/jpeg;q=0.9");
        acceps.add("image/png;q=0.9");
        acceps.add("image/*;q=0.9");
        acceps.add("*/*;q=0.8");
       
        String fileContentType = "text/plain";
        String expResult = "text/*;q=0.9";
        String result = MMTypeTools.bestMatch(acceps, fileContentType);
        assertEquals(expResult, result);
    }
    
}

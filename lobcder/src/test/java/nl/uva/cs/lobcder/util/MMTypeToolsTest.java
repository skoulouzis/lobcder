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
 * @author skoulouz
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
        
        Collection<String> supported = new ArrayList<String>();
        supported.add("text/html");
        supported.add("text/*;q=0.9");
        supported.add("image/jpeg;q=0.9");
        supported.add("image/png;q=0.9");
        supported.add("image/*;q=0.9");
        supported.add("*/*;q=0.8");
       
        String header = "text/plain";
        String expResult = "text/*;q=0.9";
        String result = MMTypeTools.bestMatch(supported, header);
        assertEquals(expResult, result);
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.MyPrincipal.Exception;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dvasunin
 */
public class MyPrincipalTest {

    @Test
    public void testGetToken() throws Exception {
        System.out.println("getToken");
        Integer[] blob = {1234};
        assertEquals("token", new MyPrincipal("token", Arrays.asList(blob)).getToken());
    }

    @Test
    public void testGetUid() throws Exception {
        System.out.println("getUid");
        Integer[] blob = {1234};
        assertEquals(1234, new MyPrincipal("token", Arrays.asList(blob)).getUid().intValue());
    }

    @Test
    public void testGetRoles() throws Exception {
        System.out.println("getRoles");
        Integer[] blob = {1234, 3, 4, 5};
        MyPrincipal instance = new MyPrincipal("token", Arrays.asList(blob));
        Integer[] roles1 = {3, 4, 5};        
        assertTrue(Arrays.asList(roles1).containsAll(instance.getRoles()) && instance.getRoles().containsAll(Arrays.asList(roles1)));       
    }
}

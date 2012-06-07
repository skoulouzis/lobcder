/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import java.util.Arrays;
import java.util.LinkedList;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.PrincipalCache;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dvasunin
 */
public class PrincipalCacheTest {
    
    @Test
    public void testCache() throws MyPrincipal.Exception, InterruptedException {
        System.out.println("testCache");
        String token = "token";
        PrincipalCache instance = new PrincipalCache();
        instance.setTimeout(1000);
        MyPrincipal principal = getPrincipal(token);
        instance.putPrincipal(principal);
        Thread.sleep(500);
        MyPrincipal result = instance.getPrincipal(token);
        assertNotNull(result);        
    }

    @Test
    public void testCacheTimeout() throws MyPrincipal.Exception, InterruptedException {
        System.out.println("testCacheTimeout");
        String token = "token";
        PrincipalCache instance = new PrincipalCache();
        instance.setTimeout(1000);
        MyPrincipal principal = getPrincipal(token);
        instance.putPrincipal(principal);
        Thread.sleep(1500);
        MyPrincipal result = instance.getPrincipal(token);
        assertNull(result);        
    }

    public MyPrincipal getPrincipal(String token) throws MyPrincipal.Exception {
        Integer[] roles = {1234, 3, 4, 5};
        MyPrincipal mp = new MyPrincipal(token, Arrays.asList(roles));
        return mp;
    }
}

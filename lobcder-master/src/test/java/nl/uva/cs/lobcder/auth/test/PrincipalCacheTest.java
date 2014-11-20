/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth.test;

import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.auth.PrincipalCache;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 * @author dvasunin
 */
public class PrincipalCacheTest {
    
    @Test
    public void testCache() throws InterruptedException {
        System.out.println("testCache");
        String token = "token";
        PrincipalCache instance = new PrincipalCache();
        MyPrincipal principal = getPrincipal("user1");
        instance.putPrincipal(token, principal, new Date().getTime() + 1000);
        Thread.sleep(500);
        MyPrincipal result = instance.getPrincipal(token);
        assertNotNull(result);        
    }

    @Test
    public void testCacheTimeout() throws InterruptedException {
        System.out.println("testCacheTimeout");
        String token = "token";
        PrincipalCache instance = new PrincipalCache();
        MyPrincipal principal = getPrincipal(token);
        instance.putPrincipal(token, principal, new Date().getTime() + 1000);
        Thread.sleep(1500);
        MyPrincipal result = instance.getPrincipal(token);
        assertNull(result);        
    }

    public MyPrincipal getPrincipal(String uname) {
        String[] roles = {"role1", "role2", "role3", "role4"};
        MyPrincipal mp = new MyPrincipal(uname, new HashSet<String>(Arrays.asList(roles)));
        return mp;
    }
}

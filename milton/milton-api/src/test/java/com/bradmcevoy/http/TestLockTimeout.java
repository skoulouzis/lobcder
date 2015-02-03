package com.bradmcevoy.http;

import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class TestLockTimeout extends TestCase {
    public void testNull() {
        LockTimeout t = LockTimeout.parseTimeout((String)null);
        assertNotNull(t);
        assertNull(t.getSeconds());
    }
    
    public void testEmpty() {
        LockTimeout t = LockTimeout.parseTimeout("");
        assertNotNull(t);
        assertNull(t.getSeconds());        
        
        t = LockTimeout.parseTimeout(" ");
        assertNotNull(t);
        assertNull(t.getSeconds());                
    }
    
    public void testSingleInfinite() {
        LockTimeout t = LockTimeout.parseTimeout("Infinite");
        assertNotNull(t);
        assertEquals((Object)Long.MAX_VALUE, t.getSeconds());        
    }

    public void testSingleSeconds() {
        LockTimeout t = LockTimeout.parseTimeout("Second-5");
        assertNotNull(t);
        assertEquals(new Long(5), t.getSeconds());        
    }
    
    public void testTwo() {
        LockTimeout t = LockTimeout.parseTimeout("Infinite, Second-5");
        assertNotNull(t);
        assertEquals((Object)Long.MAX_VALUE, t.getSeconds());        
        assertNotNull(t.getOtherSeconds());
        assertEquals(new Long(5), t.getOtherSeconds()[0]);
        
    }
    
    public void testMalformed() {
        LockTimeout t = LockTimeout.parseTimeout("Infinite Second5");
        assertNotNull(t);
        assertNull(t.getSeconds());
        assertNull(t.getOtherSeconds());
    }
}

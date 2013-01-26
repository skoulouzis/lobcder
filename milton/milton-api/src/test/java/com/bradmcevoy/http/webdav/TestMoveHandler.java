package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.webdav.Dest;
import junit.framework.TestCase;

public class TestMoveHandler extends TestCase {
    public TestMoveHandler() {
    }
    
    public void test() throws Exception {
        Dest dest;
        
        dest = new Dest("abc","/f1/f2/f3");
        assertEquals("abc",dest.host);
        assertEquals("/f1/f2",dest.url);
        assertEquals("f3",dest.name);

        dest = new Dest("abc","/f1/f2/f3/");
        assertEquals("abc",dest.host);
        assertEquals("/f1/f2",dest.url);
        assertEquals("f3",dest.name);
        
        dest = new Dest("abc","http://blah/f1/f2/f3");
        assertEquals("blah",dest.host);
        assertEquals("/f1/f2",dest.url);
        assertEquals("f3",dest.name);

        dest = new Dest("abc","http://blah:80/f1/f2/f3");
        assertEquals("blah",dest.host);
        assertEquals("/f1/f2",dest.url);
        assertEquals("f3",dest.name);        
        
        dest = new Dest("abc","/f1");
        assertEquals("abc",dest.host);
        assertEquals("/",dest.url);
        assertEquals("f1",dest.name);        
        
    }
}

package com.bradmcevoy.http;

import junit.framework.TestCase;

public class TestAbstractRequest extends TestCase{
    public TestAbstractRequest(String name) {
        super(name);
    }
    
    public void test() {
        String t = "http://abc.com/path/2";
        String s = AbstractRequest.stripToPath(t);
        assertEquals("/path/2",s);
    }
}

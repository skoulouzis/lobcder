
package com.bradmcevoy.http;

import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class AbstractRequestTest extends TestCase {
    
    public AbstractRequestTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testStripToPath() {
        String s = AbstractRequest.stripToPath("http://abc:80/my/path");
        assertEquals("/my/path", s);
    }

    public void testStripToPathWithQueryString() {
        String s = AbstractRequest.stripToPath("http://abc:80/my/path?x=y");
        assertEquals("/my/path", s);
    }

}

package com.bradmcevoy.common;

import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class PathTest extends TestCase {
    
    public PathTest(String testName) {
        super(testName);
    }

    public void test() {
        Path path = Path.path("/brad/test/1");
        System.out.println("path name: " + path.getName());

        assertEquals("1",path.getName());

        Path p2 = Path.path("/brad/test/1");
        assertEquals(path,p2);
        Path parent = Path.path("/brad/test");
        assertEquals(parent,path.getParent());
        System.out.println("----------------------");
    }

    public void testSingle() {
        Path p = Path.path("abc");
        String s = p.toString();
        assertEquals("abc",s);
    }

    public void testStrip() {
        Path path = Path.path("/a/b/c");
        Path stripped = path.getStripFirst();
        String s = stripped.toString();
        System.out.println("s: " + s);
        assertEquals("/b/c",s);
    }

    public void testAbsolute() {
        Path path = Path.path("/a/b/c");
        assertEquals(false,path.isRelative());
    }

    public void testRelative() {
        Path p1 = Path.path("test.ettrema.com:8080");
        assertEquals( 1,p1.getLength());

        Path path = Path.path("b/c");
        assertEquals(true,path.isRelative());
    }
}

package com.bradmcevoy.io;

import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class FileUtilsTest extends TestCase {
    
    public FileUtilsTest(String testName) {
        super(testName);
    }

    public static void testStripExtension() {
        assertEquals( "abc", FileUtils.stripExtension( "abc.def"));
        String s = FileUtils.stripExtension( "abc");
        assertEquals( "abc", s);
        assertEquals( "", FileUtils.stripExtension( ".def"));
        assertEquals( "abc.def", FileUtils.stripExtension( "abc.def.xxx"));
    }
}

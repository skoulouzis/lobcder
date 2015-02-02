package com.bradmcevoy.common;

import eu.medsea.mimeutil.MimeUtil;
import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class ContentTypeUtilsTest extends TestCase {
    
    public ContentTypeUtilsTest(String testName) {
        super(testName);
    }

    public void testFindContentTypes_Doc() {
        assertEquals( "application/msword", ContentTypeUtils.findContentTypes( "abc.doc"));
    }

    public void testFindContentTypes_Html() {
        assertEquals( "text/html", ContentTypeUtils.findContentTypes( "abc.html"));
    }

    public void testFindContentTypes_Txt() {
        assertEquals( "text/plain", ContentTypeUtils.findContentTypes( "abc.txt"));
    }


    public void testFindContentTypes_File() {
    }

    public void testFindAcceptableContentType() {
    }

}

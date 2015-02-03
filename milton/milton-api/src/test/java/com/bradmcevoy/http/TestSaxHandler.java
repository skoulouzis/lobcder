package com.bradmcevoy.http;


import com.bradmcevoy.http.webdav.PropFindSaxHandler;
import java.util.Map;

import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.bradmcevoy.http.LockInfo.LockScope;
import com.bradmcevoy.http.LockInfo.LockType;
import javax.xml.namespace.QName;

public class TestSaxHandler extends TestCase {
    public void testPropFind() throws Exception{
        XMLReader reader = XMLReaderFactory.createXMLReader();
        PropFindSaxHandler handler = new PropFindSaxHandler();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(PropFindSaxHandler.class.getResourceAsStream("/sample_prop_find.xml")));
        Map<QName,String> result = handler.getAttributes();
        assertEquals("httpd/unix-directory", result.get(new QName( "DAV:", "getcontenttype")));
        assertEquals("", result.get(new QName( "DAV:", "resourcetype")));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", result.get(new QName( "DAV:", "getlastmodified")));
        assertEquals("1970-01-01T00:00:00Z", result.get(new QName( "DAV:", "creationdate")));
    }
    public void testLockInfo() throws Exception{
        XMLReader reader = XMLReaderFactory.createXMLReader();
        LockInfoSaxHandler handler = new LockInfoSaxHandler();
        reader.setContentHandler(handler);
        reader.parse(new InputSource(LockInfoSaxHandler.class.getResourceAsStream("/sample_lockinfo.xml")));
        LockInfo result = handler.getInfo();
        assertEquals(result.scope,LockScope.EXCLUSIVE);
        assertEquals(result.type,LockType.WRITE);
    }
}

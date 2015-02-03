package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.webdav.PropertiesRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import javax.xml.namespace.QName;
import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class DefaultPropFindRequestFieldParserTest extends TestCase {

    DefaultPropFindRequestFieldParser fieldParser;

    String namespace = "http://ns.example.com/boxschema/";

    @Override
    protected void setUp() throws Exception {
        fieldParser = new DefaultPropFindRequestFieldParser();
    }



    public void testGetRequestedFields_SingleField() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?> " +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop xmlns:R=\"" + namespace + "\">" +
            "<R:author/> " +
            "</D:prop> " +
            "</D:propfind>";

        InputStream in = new ByteArrayInputStream( xml.getBytes());
        PropertiesRequest parseResult = fieldParser.getRequestedFields( in );
        Set<QName> set = parseResult.getNames();
        assertEquals( 1, set.size());
        QName qn = set.iterator().next();
        assertEquals( "http://ns.example.com/boxschema/", qn.getNamespaceURI());
        assertEquals( "author", qn.getLocalPart());
    }

    public void testGetRequestedFields_EmptyData() {
        String xml = "";

        InputStream in = new ByteArrayInputStream( xml.getBytes());
        PropertiesRequest parseResult = fieldParser.getRequestedFields( in );
        Set<QName> set = parseResult.getNames();
        assertEquals( 0, set.size());
    }	
}

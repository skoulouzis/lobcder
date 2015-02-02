
package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.webdav.PropPatchRequestParser.ParseResult;
import java.io.ByteArrayInputStream;
import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class DefaultPropPatchParserTest extends TestCase {
	
	private String XML_list_property = "<D:propertyupdate xmlns:D=\"DAV:\"  xmlns:Z=\"http://ns.example.com/standards/z39.50/\">\n" +
"<D:set>\n" +
"<D:prop>\n" +
"<Z:Authors>\n" +
"<Z:Author>Jim Whitehead</Z:Author>\n" +
"<Z:Author>Roy Fielding</Z:Author>\n" +
"</Z:Authors>\n" +
"</D:prop>\n" +
"</D:set>\n" +
"</D:propertyupdate>";
	
	public DefaultPropPatchParserTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetRequestedFields() {
		System.out.println(XML_list_property);
		DefaultPropPatchParser parser = new DefaultPropPatchParser();
		ParseResult result = parser.getRequestedFields(new ByteArrayInputStream(XML_list_property.getBytes()));
		assertEquals(1, result.getFieldsToSet().size());
		String s = result.getFieldsToSet().values().iterator().next();
		System.out.println(s);
	}
}

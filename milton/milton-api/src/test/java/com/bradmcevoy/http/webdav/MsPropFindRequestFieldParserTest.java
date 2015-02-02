package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.webdav.PropertiesRequest.Property;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import junit.framework.TestCase;

import static org.easymock.classextension.EasyMock.*;

/**
 *
 * @author brad
 */
public class MsPropFindRequestFieldParserTest extends TestCase {

    MsPropFindRequestFieldParser fieldParser;
    PropFindRequestFieldParser wrapped;
    InputStream request;
    Set<QName> set;

    @Override
    protected void setUp() throws Exception {
        request = createMock( InputStream.class );
        wrapped = createMock( PropFindRequestFieldParser.class );
        fieldParser = new MsPropFindRequestFieldParser( wrapped );
        set = new HashSet<QName>();
    }

    public void testGetRequestedFields_WrappedReturnsFields() {
        set.add( new QName( "a" ) );
        PropertiesRequest res = new PropertiesRequest( toProperties(set) );
        expect( wrapped.getRequestedFields( request ) ).andReturn( res );
        replay( wrapped );
        PropertiesRequest actual = fieldParser.getRequestedFields( request );

        verify( wrapped );
        assertSame( res, actual );
    }

    public void testGetRequestedFields_WrappedReturnsNothing() {
        PropertiesRequest res = new PropertiesRequest( toProperties(set) );
        expect( wrapped.getRequestedFields( request ) ).andReturn( res );
        replay( wrapped );
        PropertiesRequest actual = fieldParser.getRequestedFields( request );

        verify( wrapped );
        assertEquals( 7, actual.getNames().size() );
    }
	
	private Set<Property> toProperties(Set<QName> set) {
		Set<Property> props = new HashSet<Property>();
		for(QName n : set ) {
			props.add(new Property(n, null));
		}
		return props;
	}	
}

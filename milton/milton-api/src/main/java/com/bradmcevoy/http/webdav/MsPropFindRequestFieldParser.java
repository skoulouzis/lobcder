package com.bradmcevoy.http.webdav;

import java.io.InputStream;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * Decorator for PropFindRequestFieldParser's.
 *
 * Calls getRequestedFields on the wrapped object. If no fields were requested
 * this class adds the default ones expected
 * by windows clients. This is because windows clients generally do not
 * specify a PROPFIND body and expect the server to return these fields.
 *
 * Note that failing to return exactly the fields expected in the exact order
 * can break webdav on windows.
 *
 * @author brad
 */
public class MsPropFindRequestFieldParser implements PropFindRequestFieldParser{

    private final PropFindRequestFieldParser wrapped;

    public MsPropFindRequestFieldParser( PropFindRequestFieldParser wrapped ) {
        this.wrapped = wrapped;
    }


	@Override
    public PropertiesRequest getRequestedFields( InputStream in ) {
        PropertiesRequest result = wrapped.getRequestedFields( in );
        if( result.isAllProp() ) {
			return result;
		}
        if( result.getNames().isEmpty() ) {
            add( result, "creationdate" );
            add( result,"getlastmodified" );
            add( result,"displayname" );
            add( result,"resourcetype" );
            add( result,"getcontenttype" );
            add( result,"getcontentlength" );
            add( result,"getetag" );
        }
        return result;
    }

    private void add( PropertiesRequest result, String name ) {
        QName qname = new QName( WebDavProtocol.NS_DAV.getName(), name);
        result.add( qname );
    }

}

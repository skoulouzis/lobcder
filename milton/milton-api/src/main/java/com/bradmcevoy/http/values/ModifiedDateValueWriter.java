package com.bradmcevoy.http.values;

import com.bradmcevoy.http.DateUtils;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import java.util.Date;
import java.util.Map;

/**
 * Windows explorer is VERY picky about the format of its modified date, which
 * this class supports
 *
 * Only applies to the getlastmodified field
 *
 * @author brad
 */
public class ModifiedDateValueWriter implements ValueWriter {

    public boolean supports( String nsUri, String localName, Class c ) {
        return nsUri.equals( WebDavProtocol.NS_DAV.getName() ) && localName.equals( "getlastmodified" );
    }

    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        //sendDateProp(xmlWriter, "D:" + fieldName(), res.getModifiedDate());
        Date dt = (Date) val;
        String f;
        if( dt == null ) {
            f = "";
        } else {
            f = DateUtils.formatForWebDavModifiedDate( dt );
        }
        writer.writeProperty( prefix, localName, f );
    }

    public Object parse( String namespaceURI, String localPart, String value ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}

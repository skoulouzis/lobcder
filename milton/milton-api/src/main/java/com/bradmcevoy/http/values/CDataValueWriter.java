package com.bradmcevoy.http.values;

import com.bradmcevoy.http.Utils;
import com.bradmcevoy.http.XmlWriter;
import java.util.Map;

/**
 *
 * @author brad
 */
public class CDataValueWriter  implements ValueWriter {

    public boolean supports( String nsUri, String localName, Class c ) {
        return CData.class.equals( c );
    }

    private String nameEncode( String s ) {
        //return Utils.encode(href, false); // see MIL-31
        return Utils.escapeXml( s );
    }

    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        if( val == null ) {
            writer.writeProperty( prefix, localName );
        } else {
            CData cd = (CData) val;
            String s = nameEncode( cd.getData() );
            s = "<![CDATA[" + s + "]]>";
            writer.writeProperty( prefix, localName, s );
        }
    }

    public Object parse( String namespaceURI, String localPart, String value ) {
        return value;
    }
}

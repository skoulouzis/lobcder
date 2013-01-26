package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import java.util.Map;

public class BooleanValueWriter implements ValueWriter {

    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        Boolean b = (Boolean) val;
        writer.writeProperty( prefix, localName, b.toString().toUpperCase() );
    }

    public boolean supports( String nsUri, String localName, Class c ) {
        return c.equals( Boolean.class ) || c.equals(boolean.class);
    }

    public Object parse( String namespaceURI, String localPart, String value ) {
        if( value == null ) return false;
        value = value.toLowerCase();
        return value.equals( "t") || value.equals( "true");
    }
}

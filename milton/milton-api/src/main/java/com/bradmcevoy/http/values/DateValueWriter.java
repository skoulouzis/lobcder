package com.bradmcevoy.http.values;

import com.bradmcevoy.http.DateUtils;
import com.bradmcevoy.http.DateUtils.DateParseException;
import com.bradmcevoy.http.XmlWriter;
import java.util.Date;
import java.util.Map;

public class DateValueWriter implements ValueWriter {

	@Override
    public boolean supports( String nsUri, String localName, Class c ) {
        return Date.class.isAssignableFrom(c);
    }

	@Override
    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
        if( val == null ) {
            writer.writeProperty( prefix, localName );
        } else {
            Date date = (Date) val;
            String s = DateUtils.formatDate( date );
            writer.writeProperty( prefix, localName, s );
        }
    }

	@Override
    public Object parse( String namespaceURI, String localPart, String value ) {
        if( value == null || value.length() == 0 ) return null;
        Date dt;
        try {
            dt = DateUtils.parseDate( value );
            return dt;
        } catch( DateParseException ex ) {
            throw new RuntimeException( value, ex );
        }
    }
}

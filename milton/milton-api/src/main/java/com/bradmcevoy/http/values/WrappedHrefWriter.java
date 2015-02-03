package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import java.util.Map;

/**
 *
 * @author alex
 */
public class WrappedHrefWriter  implements ValueWriter {
    public boolean supports( String nsUri, String localName, Class c ) {
        return WrappedHref.class.isAssignableFrom( c );
    }

    public void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes ) {
      writer.open(prefix, localName);
      WrappedHref wrappedHref = (WrappedHref) val;
      if( wrappedHref != null && wrappedHref.getValue() != null ) {
            //TODO: Replace explicit namespace declaration with reference to constant
            Element hrefEl = writer.begin(WebDavProtocol.NS_DAV.getPrefix(),"href" ).open();
            hrefEl.writeText( wrappedHref.getValue() );
            hrefEl.close();
      }
      writer.close(prefix, localName);
    }

    public Object parse( String namespaceURI, String localPart, String value ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import java.util.Map;

/**
 * An implementation of ValueWriter will generate xml for some particular property
 * or type, and be able to parse proppatch textual values into its typed form
 *
 * Generally ValueWriter's should be symmetrical, in that they can parse what
 * they generate.
 *
 * @author brad
 */
public interface ValueWriter {

    /**
     * Does this ValueWriter support the data type or property for writing xml
     *
     * @param prefix
     * @param nsUri
     * @param localName
     * @param val
     * @return
     */
    boolean supports( String nsUri, String localName, Class valueClass );

    /**
     * Write the value out to XML using the given XmlWriter
     *
     * @param writer
     * @param nsUri
     * @param prefix
     * @param localName
     * @param val
     * @param href
     * @param nsPrefixes
     */
    void writeValue( XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes );

    /**
     * Parse the given textual representation, probably from a PROPPATCH request
     *
     * @param namespaceURI
     * @param localPart
     * @param value
     * @return
     */
    Object parse( String namespaceURI, String localPart, String value );
}

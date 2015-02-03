package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import java.util.Map;
import java.util.UUID;

/**
 *
 */
public class UUIDValueWriter implements ValueWriter {

    public void writeValue(XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes) {
        UUID b = (UUID) val;
        writer.writeProperty(prefix, localName, b.toString());
    }

    public boolean supports(String nsUri, String localName, Class c) {
        return c.equals(UUID.class);
    }

    public Object parse(String namespaceURI, String localPart, String value) {
        if (value == null) {
            return false;
        }
        return UUID.fromString(value);
    }
}

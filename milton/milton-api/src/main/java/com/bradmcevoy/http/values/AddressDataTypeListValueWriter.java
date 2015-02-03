package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import java.util.Map;

/**
 * Supports AddressDataTypeList objects, and writes them out as a list of  
 * <C:address-data-type content-type="text/vcard" version="3.0"/> 
 * elements
 * 
 * @author nabil.shams
 */
public class AddressDataTypeListValueWriter implements ValueWriter {

	@Override
	public boolean supports(String nsUri, String localName, Class c) {
		boolean b = AddressDataTypeList.class.isAssignableFrom(c);
		return b;
	}

	@Override
	public void writeValue(XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes) {		
		if (val instanceof AddressDataTypeList) {
			Element parent = writer.begin(prefix, localName).open();
			AddressDataTypeList list = (AddressDataTypeList) val;
			if (list != null) {
				for (Pair<String, String> pair : list) {
					Element child = writer.begin(prefix + ":address-data-type").open(false);
					child.writeAtt("content-type", pair.getObject1());
					child.writeAtt("version", pair.getObject2());
					child.close();
				}
			}
			parent.close();
		} else {
			if (val != null) {
				throw new RuntimeException("Value is not correct type. Is a: " + val.getClass());
			}
		}
	}

	@Override
	public Object parse(String namespaceURI, String localPart, String value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}

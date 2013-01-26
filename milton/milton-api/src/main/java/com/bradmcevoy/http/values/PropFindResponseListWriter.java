package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import com.bradmcevoy.http.webdav.PropFindResponse;
import com.bradmcevoy.http.webdav.PropFindXmlGeneratorHelper;
import java.util.Map;

/**
 *
 * @author bradm
 */
public class PropFindResponseListWriter  implements ValueWriter {

	private final PropFindXmlGeneratorHelper propFindXmlGeneratorHelper;

	public PropFindResponseListWriter(PropFindXmlGeneratorHelper propFindXmlGeneratorHelper) {
		this.propFindXmlGeneratorHelper = propFindXmlGeneratorHelper;
	}
	
	
	@Override
	public boolean supports(String nsUri, String localName, Class c) {
		return PropFindResponseList.class.isAssignableFrom(c);
	}

	@Override
	public void writeValue(XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes) {
		Element outerEl = writer.begin(prefix, localName).open();
		PropFindResponseList list = (PropFindResponseList) val;
		if (list != null) {
			for (PropFindResponse s : list) {
				propFindXmlGeneratorHelper.appendResponse(writer, s, nsPrefixes);
			}
		}
		outerEl.close();
	}

	@Override
	public Object parse(String namespaceURI, String localPart, String value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}

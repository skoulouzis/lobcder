package com.bradmcevoy.http.values;

import com.ettrema.http.AccessControlledResource.Priviledge;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.XmlWriter.Element;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.ettrema.http.acl.PriviledgeList;
import java.util.EnumMap;
import java.util.Map;

/**
 * Supports PriviledgeList objects, and writes them out as a list of
 * <privilege>...</privilege> elements.
 *
 * Currently readonly but should support writing.
 *
 * @author avuillard
 */
public class PriviledgeListValueWriter implements ValueWriter {

	private static Map<Priviledge, String> priviledgeToStringMap = PriviledgeListValueWriter.initPriviledgeToStringMap();

	private static Map<Priviledge, String> initPriviledgeToStringMap() {
		Map<Priviledge, String> map = new EnumMap<Priviledge, String>(Priviledge.class);
		map.put(Priviledge.READ, "read");
		map.put(Priviledge.WRITE, "write");
		map.put(Priviledge.READ_ACL, "read-acl");
		map.put(Priviledge.WRITE_ACL, "write-acl");
		map.put(Priviledge.UNLOCK, "unlock");
		map.put(Priviledge.READ_CURRENT_USER_PRIVILEDGE, "read-current-user-privilege-set");
		map.put(Priviledge.WRITE_PROPERTIES, "write-properties");
		map.put(Priviledge.WRITE_CONTENT, "write-content");
		map.put(Priviledge.BIND, "bind");
		map.put(Priviledge.UNBIND, "unbind");
		map.put(Priviledge.ALL, "all");
		return map;
	}

	@Override
	public boolean supports(String nsUri, String localName, Class c) {
		return PriviledgeList.class.isAssignableFrom(c);
	}

	@Override
	public void writeValue(XmlWriter writer, String nsUri, String prefix, String localName, Object val, String href, Map<String, String> nsPrefixes) {
		if (val instanceof PriviledgeList) {
			PriviledgeList list = (PriviledgeList) val;
			Element outerEl = writer.begin(prefix, localName).open();
			if (list != null) {
				for (Priviledge p : list) {
					String privilegeString = PriviledgeListValueWriter.priviledgeToStringMap.get(p);
					if (privilegeString == null) {
						continue;
					}

					Element privilegeEl = writer.begin(WebDavProtocol.DAV_PREFIX + ":privilege").open(false);
					Element privilegeValueEl = privilegeEl.begin(WebDavProtocol.DAV_PREFIX, privilegeString);
					privilegeValueEl.noContent();
					privilegeEl.close();
				}
			}
			outerEl.close();
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
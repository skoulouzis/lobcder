package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.XmlWriter;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public interface PropertyWriter {
    boolean supports(QName name, Class valueType);

    void write(XmlWriter xmlWriter, QName name, Object val);
}

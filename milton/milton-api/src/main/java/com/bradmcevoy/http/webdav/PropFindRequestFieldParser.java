package com.bradmcevoy.http.webdav;

import java.io.InputStream;

/**
 * Parses the body of a PROPFIND request and returns the requested fields
 *
 * @author brad
 */
public interface PropFindRequestFieldParser {

    PropertiesRequest getRequestedFields( InputStream in );
}

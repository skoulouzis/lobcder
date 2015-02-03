package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.Resource;
import java.util.Set;

/**
 * For all webdav extensions to implement. Provides hooks into PROPFIND
 * and PROPPATCH handling
 *
 * @author brad
 */
public interface WebDavExtension {
  void appendSupportedLevels(Resource r, Set<String> supportedLevels);
}

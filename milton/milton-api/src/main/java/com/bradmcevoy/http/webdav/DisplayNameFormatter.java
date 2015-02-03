package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.PropFindableResource;

/**
 * This interface serves to allow users of milton to implement different
 * display name strategies.
 *
 * The display name is completely arbitrary, ie it is not in any way necessarily
 * related to the actual name used to contruct the href.
 *
 * This class also serves as a mechanism for deciding whether to wrap the display
 * name in a CDATA element.
 *
 * @author brad
 */
public interface DisplayNameFormatter {
    /**
     * Generate the exact text to appear inside display name elements. No
     * further encoding of this text is applied when generating the xml.
     *
     * @param res
     * @return
     */
    String formatDisplayName(PropFindableResource res);
}

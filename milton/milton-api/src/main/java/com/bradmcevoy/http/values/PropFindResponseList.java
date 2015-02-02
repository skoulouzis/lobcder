package com.bradmcevoy.http.values;

import com.bradmcevoy.http.webdav.PropFindResponse;
import java.util.ArrayList;

/**
 * Represents a list of responses.
 * 
 * Note you can't just have a genericed list because we need to know the type
 * of the list at runtime (for valuewriter determination) but generic information
 * is removed at compile time.
 *
 * @author bradm
 */
public class PropFindResponseList  extends ArrayList<PropFindResponse> {

    private static final long serialVersionUID = 1L;


	
}

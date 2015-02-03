package com.bradmcevoy.http.values;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Holds a list of href values which will be written as a list of <href> elements
 *
 * See HrefListValueWriter
 *
 * @author brad
 */
public class HrefList extends ArrayList<String> {

    private static final long serialVersionUID = 1L;

    public static HrefList asList(String... items) {
        HrefList l = new HrefList();
		l.addAll(Arrays.asList(items));
        return l;
    }
}

package com.bradmcevoy.http.values;

/**
 * A type of value which will be written into a CDATA section by its
 * ValueWriter
 *
 * @author brad
 */
public class CData {
    private final String data;

    public CData( String data ) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}

package com.bradmcevoy.http;

/**
 * Represents support for a given property. The property may be null, blank
 * or have a value
 *
 */
public interface CustomProperty {

    /**
     * Returns a class which is assignable from any value which can be stored
     * in this property. This should be sufficient to determine a ValueWriter
     * to parse a PROPPATCH value.
     * 
     * @return
     */
    Class getValueClass();

    /**
     * Returns the typed value. It should be assumed that this value could
     * be serialised, although it doesnt require the Serializable interface
     *
     * @return
     */
    Object getTypedValue();

    /**
     * Returns a textual representation of the value suitable for consumption
     * by wedav clients, except that it should not be character encoded as
     * milton will do that
     *
     * @return
     */
    String getFormattedValue();

    /**
     * Set the unencoded string value into this property. This may include
     * parsing if this is a typed property.
     *
     * @param s
     */
    void setFormattedValue( String s );
}

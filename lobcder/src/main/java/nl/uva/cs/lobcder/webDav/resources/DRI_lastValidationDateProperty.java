/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.CustomProperty;

/**
 *
 * @author S. koulouzis
 */
class DRI_lastValidationDateProperty implements CustomProperty{
    private Long value;

    DRI_lastValidationDateProperty() {
        value =  Long.valueOf(0);
    }
    
    DRI_lastValidationDateProperty(Long checksum) {
        this.value = checksum;
    }

    @Override
    public Class getValueClass() {
        return DRI_lastValidationDateProperty.class;
    }

    @Override
    public Object getTypedValue() {
        return value;
    }

    @Override
    public String getFormattedValue() {
        return value.toString();
    }

    @Override
    public void setFormattedValue(String v) {
        this.value = Long.valueOf(v);
    }    
}

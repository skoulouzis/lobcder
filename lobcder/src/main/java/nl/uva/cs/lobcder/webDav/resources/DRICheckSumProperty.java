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
class DRICheckSumProperty implements CustomProperty{
    private Long value;

    DRICheckSumProperty() {
        value =  Long.valueOf(0);
    }
    
    DRICheckSumProperty(Long checksum) {
        this.value = checksum;
    }

    @Override
    public Class getValueClass() {
        return DRICheckSumProperty.class;
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
        System.err.println("AAAAAAAAAAAAa: " + v);
        this.value = Long.valueOf(v);
    }    
}

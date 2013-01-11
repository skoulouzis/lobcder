/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.CustomProperty;
import com.bradmcevoy.property.PropertySource;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author S. koulouzis
 */
class DRICheckSumProperty implements CustomProperty {

    private LogicalData ld;

//    public DRICheckSumProperty(PropertySource.PropertyAccessibility propertyAccessibility, Class<Object> aClass, LogicalData ld) {
//        super(propertyAccessibility, aClass);
//        this.ld = ld;
//    }
    DRICheckSumProperty(LogicalData ld) {
        this.ld = ld;
    }

    @Override
    public Class getValueClass() {
        return DRICheckSumProperty.class;
    }

    @Override
    public Object getTypedValue() {
        return ld.getChecksum();
    }

    @Override
    public String getFormattedValue() {
        return String.valueOf(ld.getChecksum());
    }

    @Override
    public void setFormattedValue(String v) {
        if (v != null) {
            ld.updateChecksum(Long.valueOf(v));
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.CustomProperty;
import nl.uva.cs.lobcder.resources.LogicalData;

/**
 *
 * @author S. koulouzis
 */
class DRIsSupervisedProperty implements CustomProperty{

    private LogicalData ld;
    
    DRIsSupervisedProperty(LogicalData ld) {
        this.ld = ld;
    }

    @Override
    public Class getValueClass() {
        return DRIsSupervisedProperty.class;
    }

    @Override
    public Object getTypedValue() {
        return ld.getSupervised();
    }

    @Override
    public String getFormattedValue() {
        return ld.getSupervised().toString();
    }

    @Override
    public void setFormattedValue(String v) {
        ld.updateSupervised(Boolean.valueOf(v));
    }    
}

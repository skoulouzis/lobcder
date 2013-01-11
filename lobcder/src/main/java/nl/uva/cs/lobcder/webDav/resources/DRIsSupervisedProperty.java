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
class DRIsSupervisedProperty implements CustomProperty {

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
        return String.valueOf(ld.getSupervised());
    }

    @Override
    public void setFormattedValue(String v) {
        debug("setFormattedValue: " + v);
        if (v != null) {
            ld.updateSupervised(Boolean.valueOf(v));
        }
    }

    private void debug(String msg) {
        System.err.println(ld.getLDRI() + ": " + msg);
    }
}

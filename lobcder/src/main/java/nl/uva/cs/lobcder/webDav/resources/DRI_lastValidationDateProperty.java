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
class DRI_lastValidationDateProperty implements CustomProperty{
    
    private LogicalData ld;

    DRI_lastValidationDateProperty(LogicalData ld) {
        this.ld = ld;
    }
   

    @Override
    public Class getValueClass() {
        return DRI_lastValidationDateProperty.class;
    }

    @Override
    public Object getTypedValue() {
        return ld.getLastValidationDate();
    }

    @Override
    public String getFormattedValue() {
        return ld.getLastValidationDate().toString();
    }

    @Override
    public void setFormattedValue(String v) {
        ld.updateLastValidationDate(Long.valueOf(v));
    }    
}

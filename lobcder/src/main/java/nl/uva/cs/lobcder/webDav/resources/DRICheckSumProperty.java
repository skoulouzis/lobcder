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
class DRICheckSumProperty implements CustomProperty{
    
    private LogicalData ld;

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
        return ld.getChecksum().toString();
    }

    @Override
    public void setFormattedValue(String v) {
        if(v !=null){
         ld.updateChecksum(Long.valueOf(v));   
        }
    }    
}

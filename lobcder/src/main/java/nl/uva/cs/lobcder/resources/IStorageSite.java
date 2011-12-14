/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.Collection;

/**
 *
 * @author S. Koulouzis
 */
public interface IStorageSite {

    String getEndpoint();

    String getVPHUsername();

    Collection<String> getLogicalPaths();
    
    boolean LDRIHasPhysicalData(Path ldri);
}

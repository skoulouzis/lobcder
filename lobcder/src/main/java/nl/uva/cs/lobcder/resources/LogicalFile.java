/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author S. Koulouzis
 */

@PersistenceCapable
public class LogicalFile extends LogicalData {

    /**
     * 
     */
    private static final long serialVersionUID = 6019657958412257303L;
    private List<StorageSite> accessLocations;

    public LogicalFile(Path logicalResourceName) throws IOException {
        super(logicalResourceName);
    }

    public void addAccessLocations(List<StorageSite> accessLocations) {
        if (this.accessLocations == null) {
            this.accessLocations = new ArrayList<StorageSite>();
        }
        this.accessLocations.addAll(accessLocations);
    }

    public void removeAccessLocations(List<StorageSite> accessLocations) {
        if (this.accessLocations != null && !this.accessLocations.isEmpty()) {
            this.accessLocations.removeAll(accessLocations);
        }
    }

    public void removeAllAccessLocations() {
        if (this.accessLocations != null && !this.accessLocations.isEmpty()) {
            this.accessLocations.clear();
        }
    }

    public List<StorageSite> getAccessLocations() {
        return accessLocations;
    }

    public void addAccessLocation(StorageSite accessLocation) {
        if (this.accessLocations == null) {
            this.accessLocations = new ArrayList<StorageSite>();
        }
        this.accessLocations.add(accessLocation);
    }
}

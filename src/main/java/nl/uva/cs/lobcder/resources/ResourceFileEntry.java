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
public class ResourceFileEntry extends DataResourceEntry {

    /**
     * 
     */
    private static final long serialVersionUID = 6019657958412257303L;
    private List<StorageResource> accessLocations;

    public ResourceFileEntry(Path logicalResourceName) throws IOException {
        super(logicalResourceName);
    }

    public void addAccessLocations(List<StorageResource> accessLocations) {
        if (this.accessLocations == null) {
            this.accessLocations = new ArrayList<StorageResource>();
        }
        this.accessLocations.addAll(accessLocations);
    }

    public void removeAccessLocations(List<StorageResource> accessLocations) {
        if (this.accessLocations != null && !this.accessLocations.isEmpty()) {
            this.accessLocations.removeAll(accessLocations);
        }
    }

    public void removeAllAccessLocations() {
        if (this.accessLocations != null && !this.accessLocations.isEmpty()) {
            this.accessLocations.clear();
        }
    }

    public List<StorageResource> getAccessLocations() {
        return accessLocations;
    }

    public void addAccessLocation(StorageResource accessLocation) {
        if (this.accessLocations == null) {
            this.accessLocations = new ArrayList<StorageResource>();
        }
        this.accessLocations.add(accessLocation);
    }
}

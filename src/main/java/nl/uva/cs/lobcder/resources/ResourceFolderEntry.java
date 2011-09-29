/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.IOException;
import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author S. Koulouzis
 */

@PersistenceCapable
public class ResourceFolderEntry extends DataResourceEntry {

    /**
     * 
     */
    private static final long serialVersionUID = -2765285570381475790L;

    public ResourceFolderEntry(Path logicalResourceName) throws IOException {
        super(logicalResourceName);
    }

    public IDataResourceEntry getChildByLRN(String name) {
        for (IDataResourceEntry r : getChildren()) {
            if (r.getLDRI().equals(name)) {
                return r;
            }
        }
        return null;
    }

    public IDataResourceEntry getChildByUID(String uid1) {
        for (IDataResourceEntry r : getChildren()) {
            if (r.getUID().equals(uid1)) {
                return r;
            }
        }
        return null;
    }

    public Boolean hasChildren() {
        if (this.getChildren() == null || this.getChildren().isEmpty()
                || getChildren().size() <= 0) {
            return false;
        }
        return true;
    }
}

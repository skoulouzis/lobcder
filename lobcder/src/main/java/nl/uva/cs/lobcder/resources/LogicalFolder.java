/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.io.IOException;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author S. Koulouzis
 */

@PersistenceCapable
@Inheritance(strategy= InheritanceStrategy.NEW_TABLE )
public class LogicalFolder extends LogicalData {

    /**
     * 
     */
    private static final long serialVersionUID = -2765285570381475790L;

    public LogicalFolder(Path logicalResourceName) throws IOException {
        super(logicalResourceName);
    }
}

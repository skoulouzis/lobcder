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

    public LogicalFile(Path logicalResourceName) throws IOException {
        super(logicalResourceName);
    }
}

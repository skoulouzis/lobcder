/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.exceptions;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.MiltonException;

/**
 *
 * source from: http://svn.dcache.org/dCache/trunk/modules/dCache/
 */
public class ForbiddenException extends WebDavException {
    private final String message;

    public ForbiddenException(Resource resource) {
        super(resource);
        this.message = "Forbidden exception: " + resource.getName();
    }
    
        @Override
    public String getMessage() {
        return this.message;
    }
        
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webdav.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *
 * source from: http://svn.dcache.org/dCache/trunk/modules/dCache/
 */
public class UnauthorizedException extends WebDavException
{
    public UnauthorizedException(Resource resource)
    {
        super(resource);
    }

    public UnauthorizedException(String message, Resource resource)
    {
        super(message, resource);
    }

    public UnauthorizedException(String message, Throwable cause, Resource resource)
    {
        super(message, cause, resource);
    }
}

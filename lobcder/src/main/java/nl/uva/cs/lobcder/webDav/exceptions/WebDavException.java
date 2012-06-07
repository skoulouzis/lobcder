/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *
 * source from: http://svn.dcache.org/dCache/trunk/modules/dCache/
 */
public class WebDavException extends RuntimeException
{
    private final Resource _resource;

    public WebDavException(Resource resource)
    {
        _resource = resource;
    }

    public WebDavException(String message, Resource resource)
    {
        super(message);
        _resource = resource;
    }

    public WebDavException(String message, Throwable cause, Resource resource)
    {
        super(message, cause);
        _resource = resource;
    }

    public Resource getResource()
    {
        return _resource;
    }
}
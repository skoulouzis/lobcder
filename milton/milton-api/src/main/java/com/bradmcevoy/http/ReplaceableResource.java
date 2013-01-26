package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.InputStream;

/**
 * Indicates a resource which can have its content replaced by a PUT method
 *
 * @author brad
 */
public interface ReplaceableResource extends Resource {

    public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException;

}

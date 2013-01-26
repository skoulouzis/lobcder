package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * webDAV MOVE
 */
public interface MoveableResource  extends Resource {

    /**
     *
     * @param rDest is the destination folder to move to.
     * @param name is the new name of the moved resource
     * @throws ConflictException if the destination already exists, or the operation
     * could not be completed because of some other persisted state
     */
    void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException;
    
}

package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * Represents a collection (ie a folder or directory which allows sub collections
 * to be created
 *
 * @author brad
 */
public interface MakeCollectionableResource extends CollectionResource {
    /**
     *
     * @param newName
     * @return
     * @throws NotAuthorizedException
     * @throws ConflictException
     * @throws BadRequestException
     */
    CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException;
    
}

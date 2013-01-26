package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.event.EventManager;

/**
 * Supporting functions for the DeleteHandler
 *
 */
public interface DeleteHelper {
    /**
     * Check if the resource or any child resources are locked or otherwise not
     * deletable
     *
     * @param req
     * @param r
     * @return
     */
    boolean isLockedOut(Request req, Resource r) throws NotAuthorizedException, BadRequestException;

    /**
     * Delete the resource and any child resources
	 * 
	 * The implementation should fire delete events for all resources physically
	 * deleted.
     *
     * @param r
     */
    void delete(DeletableResource r, EventManager eventManager) throws NotAuthorizedException, ConflictException, BadRequestException;
}

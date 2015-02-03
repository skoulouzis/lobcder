package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * Implement this to allow your resource to be deleted by webdav clients.
 *
 * Milton will ensure there are no locks which prevent the delete, however the
 * current user might have the resource locked in which case your implementation
 *
 * Usually milton will recursively call delete on all children within a collection
 * being deleted. However you can prevent this my implementing DeletableCollectionResource
 * which causes milton to ONLY call delete on the specific resource being deleted. In
 * which case it is your responsibility to test for locks on all child resources
 * 
 */
public interface DeletableResource extends Resource{

    /**
     * Non-recursive delete of this resource. Milton will call delete on child
     * resources first.
     *
     * @throws NotAuthorizedException - if the operation should not be permitted for security reasons
     * @throws ConflictException - if there is some pre-condition that has not been met, or there is
     * aspect of the resource state which prevents the resource from being deleted
     * @throws BadRequestException - if there is some aspect of the request which means
     * it is not sufficient to perform a delete.
     */
    void delete() throws NotAuthorizedException, ConflictException, BadRequestException;
    
}

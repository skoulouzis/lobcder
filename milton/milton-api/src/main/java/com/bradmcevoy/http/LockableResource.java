package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.LockedException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;

/**
 * webDAV LOCK
 *
 * You should also implement LockingCollectionResource on your collections for full
 * locking support
 * 
 * @author brad
 */
public interface LockableResource extends Resource {
    /**
     * Lock this resource and return a token
     * 
     * @param timeout - in seconds, or null
     * @param lockInfo
     * @return - a result containing the token representing the lock if succesful,
     * otherwise a failure reason code
     */
    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException, PreConditionFailedException, LockedException;
    
    /**
     * Renew the lock and return new lock info
     * 
     * @param token
     * @return
     */
    public LockResult refreshLock(String token) throws NotAuthorizedException, PreConditionFailedException;

    /**
     * If the resource is currently locked, and the tokenId  matches the current
     * one, unlock the resource
     *
     * @param tokenId
     */
    public void unlock(String tokenId) throws NotAuthorizedException, PreConditionFailedException;

    /**
     *
     * @return - the current lock, if the resource is locked, or null
     */
    public LockToken getCurrentLock();
}

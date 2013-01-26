package com.ettrema.http.fs;

import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 *
 */
public interface  LockManager {

    LockResult lock(LockTimeout timeout, LockInfo lockInfo, LockableResource resource) throws NotAuthorizedException;

    LockResult refresh(String token, LockableResource resource) throws NotAuthorizedException;

    void unlock(String tokenId, LockableResource resource) throws NotAuthorizedException;

    LockToken getCurrentToken(LockableResource resource);

}


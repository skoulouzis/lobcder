package com.bradmcevoy.http.quota;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

/**
 * Implementations will check some aspect of whether or not its allowable
 * to load some content into the system. This may be whether there is sufficient
 * disk space, or whether the user's quota is full.
 *
 * This is generally called on a PUT, so there are 2 possibilities:
 *   a. the PUT is to an existing resource which will be replaced. Even if a
 * quota is currently exceeded this might be allowed if the new resource is no
 * larger then the one it is replacing
 *
 *   b. the PUT is to create a new resource. In this case it is simply an add,
 * but the parent folder might or might not exist.
 *
 * @author brad
 */
public interface StorageChecker {
    public enum StorageErrorReason {
        SER_QUOTA_EXCEEDED,
        SER_DISK_FULL
    }

    /**
     * Check to see if the operation should be allowed, when an existing resource
     * is to be overwritten or replaced.
     *
     * @param request
     * @param replaced - the resource being replaced
     * @param host
     * @return - null if the operation should proceed, otherwise a reason for the error
     */
    StorageErrorReason checkStorageOnReplace(Request request, CollectionResource parent, Resource replaced, String host);

    /**
     * Check to see if the operation should be allowed, when there is no existing
     * resource. The parent collection may or may not exist, so only its path is
     * provided.
     *
     * @param request
     * @param parentPath - the path to the parent collection. E.g. http://abc.com/path = /path
     * @param host
     * @return - null if the operation should proceed, or the reason for the failure
     */
    StorageErrorReason checkStorageOnAdd(Request request, CollectionResource nearestParent, Path parentPath, String host);
}

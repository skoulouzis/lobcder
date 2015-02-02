package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 * Thrown when there is an attempt to lock an already locked resource
 *
 * @author brad
 */
public class LockedException extends MiltonException {
    private static final long serialVersionUID = 1L;

    public LockedException(Resource r) {
        super(r);
    }

}

package com.bradmcevoy.http.exceptions;

import com.bradmcevoy.http.Resource;

/**
 *
 * @author brad
 */
public class PreConditionFailedException extends MiltonException{
    private static final long serialVersionUID = 1L;

    public PreConditionFailedException(Resource r) {
        super(r);
    }
}

package com.ettrema.event;

import com.bradmcevoy.http.Resource;


/**
 *
 * @author brad
 */
public class DeleteEvent implements ResourceEvent{
    private final Resource res;

    public DeleteEvent( Resource res ) {
        this.res = res;
    }

    @Override
    public Resource getResource() {
        return res;
    }

}

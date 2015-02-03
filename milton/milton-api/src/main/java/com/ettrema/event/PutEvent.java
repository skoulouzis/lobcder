package com.ettrema.event;

import com.bradmcevoy.http.Resource;

/**
 *
 * @author brad
 */
public class PutEvent implements ResourceEvent {
    private final Resource res;

    public PutEvent( Resource res ) {
        this.res = res;
    }

    @Override
    public Resource getResource() {
        return res;
    }


}

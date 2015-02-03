package com.ettrema.event;

import com.bradmcevoy.http.Resource;

/**
 *
 * @author brad
 */
public class GetEvent  implements ResourceEvent {
    private final Resource res;

    public GetEvent( Resource res) {
        this.res = res;
    }


    @Override
    public Resource getResource() {
        return res;
    }
}

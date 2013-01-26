package com.ettrema.event;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.PropFindResponse;

/**
 *
 * @author brad
 */
public class PropPatchEvent implements ResourceEvent {
    private final Resource res;
    private final PropFindResponse resp;

    public PropPatchEvent( Resource res, PropFindResponse resp ) {
        this.res = res;
        this.resp = resp;
    }


    @Override
    public Resource getResource() {
        return res;
    }

    public PropFindResponse getResponse() {
        return resp;
    }



}

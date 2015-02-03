package com.ettrema.event;

import com.bradmcevoy.http.Request;

/**
 *
 * @author brad
 */
public class RequestEvent implements Event {
    private final Request request;

    public RequestEvent(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }


}

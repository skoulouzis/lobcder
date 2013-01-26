package com.ettrema.event;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Response;

/**
 * Fired after response is complete
 *
 * @author brad
 */
public class ResponseEvent implements Event {
    private final Request request;
    private final Response response;

    public ResponseEvent(Request request, Response response) {
        this.request = request;
        this.response = response;
    }


    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }        
}

package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;

/**
 * Used for when we want to delegate POST handling to something other then the
 * usual processForm method.
 *
 * For example, this can be for handling POST requests to scheduling resources
 * with a content type of text/calendar, in which case we should perform
 * specific scheduling logic instead of artbitrary operations which
 * are usually implemented on POST requests
 *
 * @author brad
 */
public interface CustomPostHandler {
    boolean supports(Resource resource, Request request);

    void process(Resource resource, Request request, Response response);
}

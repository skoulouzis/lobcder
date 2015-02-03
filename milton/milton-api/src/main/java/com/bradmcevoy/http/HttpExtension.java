package com.bradmcevoy.http;

import com.bradmcevoy.http.http11.CustomPostHandler;
import java.util.List;
import java.util.Set;

/**
 *
 * @author brad
 */
public interface HttpExtension {

    /**
     *
     * @return - all method handlers that this extension supports.
     */
    Set<Handler> getHandlers();

    List<CustomPostHandler> getCustomPostHandlers();

//    public void setResponseHeaders( Request request, Response response, Resource resource, Status status );
}

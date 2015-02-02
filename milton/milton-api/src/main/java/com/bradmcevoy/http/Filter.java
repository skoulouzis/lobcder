package com.bradmcevoy.http;

/** Interface for a request/response wrapping filter.
 *  <P/>
 *  Add these with HttpManager.addFilter(ordinal,filter)
 *  <P/>
 *  By default the manager loads a single filter which delegates the
 *  request to a handler appropriate for the request method
 *  <P/>
 *  Users can add their own for logging, security, managing database connections etc
 *
 */
public interface Filter {
    public void process(FilterChain chain, Request request, Response response);   
}

package com.bradmcevoy.http;

/** Passes the request and response along a series of filters
 *
 *  By default the HttpManager loads a single filter which executes the appropriate
 *  handler for the http method
 *
 *  Additional filters can be added using HttpManager.addFilter
 */
public class FilterChain {
    
    final HttpManager httpManager;
    int pos = 0;
    
    public FilterChain(HttpManager httpManager) {
        this.httpManager = httpManager;
    }

    public void process( Request request, Response response) {        
        Filter filter = httpManager.filters.get(pos++);
        filter.process(this,request,response);
    }

    public HttpManager getHttpManager() {
        return httpManager;
    }
       
}

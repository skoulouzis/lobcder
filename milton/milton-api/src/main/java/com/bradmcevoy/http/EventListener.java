package com.bradmcevoy.http;

import java.util.Map;

/**
 *
 * @author brad
 * 
 * 
 * 
 */
public interface EventListener {

    void onPost(Request request, Response response, Resource resource, Map<String, String> params, Map<String, FileItem> files);
    
    void onGet(Request request, Response response, Resource resource, Map<String, String> params);
    
    void onProcessResourceStart(Request request, Response response, Resource resource);
    
    void onProcessResourceFinish(Request request, Response response, Resource resource, long duration);
    
    
}

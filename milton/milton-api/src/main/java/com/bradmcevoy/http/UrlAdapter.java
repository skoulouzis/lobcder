package com.bradmcevoy.http;

/**
 * Used to transform the requested url prior to resource location
 *
 * @author brad
 */
public interface  UrlAdapter {
    String getUrl(Request request);
}

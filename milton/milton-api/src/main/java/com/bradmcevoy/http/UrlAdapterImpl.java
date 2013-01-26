package com.bradmcevoy.http;

/**
 *
 * @author brad
 */
public class UrlAdapterImpl implements UrlAdapter{
    public String getUrl(Request request) {
        String s = HttpManager.decodeUrl( request.getAbsolutePath() );
        if( s.contains( "/DavWWWRoot")) {
            return s.replace( "/DavWWWRoot", "");
        } else {
            return s;
        }
    }

}

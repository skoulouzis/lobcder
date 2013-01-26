package com.bradmcevoy.http.http11;

/**
 * For response handlers that can be buffered
 *
 * @author brad
 */
public interface Bufferable {
	public DefaultHttp11ResponseHandler.BUFFERING getBuffering() ;

	public void setBuffering(DefaultHttp11ResponseHandler.BUFFERING buffering);
}

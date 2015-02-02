package com.bradmcevoy.http;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used the decorator pattern to add support for .wellknown paths to a custom
 * resource factory.
 * 
 * By integrating this into your application it will "overlay" some additional
 * synthetic resources (ie url's) on your application
 * 
 * This is required for iCal5+ support for caldav
 * 
 * http://tools.ietf.org/html/draft-daboo-srv-caldav-10#page-5
 * http://tools.ietf.org/html/rfc5785
 * http://hueniverse.com/2010/04/rfc-5785-defining-well-known-uris/
 *
 * @author brad
 */
public class WellKnownResourceFactory implements ResourceFactory {

	public static final String URI_PREFIX = "/.well-known";
	
	private final ResourceFactory wrapped;
	
	private Map<String,WellKnownHandler> mapOfWellKnownHandlers = new HashMap<String, WellKnownHandler>();

	public WellKnownResourceFactory(ResourceFactory wrapped, List<WellKnownHandler> wellKnownHandlers) {
		this.wrapped = wrapped;
		for( WellKnownHandler h : wellKnownHandlers ) {
			addHandler(h);
		}
	}
//		
//	public WellKnownResourceFactory(ResourceFactory wrapped) {
//		this.wrapped = wrapped;
//	}	
	
	@Override
	public Resource getResource(String host, String sPath) throws NotAuthorizedException, BadRequestException {
		if( sPath.startsWith(URI_PREFIX)) {
			Path path = Path.path(sPath);
			path = path.getStripFirst();
			WellKnownHandler wellKnown = mapOfWellKnownHandlers.get(path.getFirst());
			if( wellKnown != null ) {
				Resource hostRes = wrapped.getResource(host, "/");
				if( hostRes != null ) {
					return wellKnown.locateWellKnownResource(hostRes);
				}
			}
		}
		return wrapped.getResource(host, sPath);
	}
	
	public final void addHandler(WellKnownHandler handler) {
		mapOfWellKnownHandlers.put(handler.getWellKnownName(), handler);
	}
	
	/**
	 * Locates a resource for a .well-known/XXX path
	 */
	public interface WellKnownHandler {
		/**
		 * Identifies the part of the path following .well-known which will map to this handler
		 * 
		 * @return 
		 */
		String getWellKnownName();
		
		/**
		 * Find a resource which will handle the well-known request. This should
		 * generally redirect to the appropriate location. It should not perform
		 * the role of the service being looked up.
		 */
		Resource locateWellKnownResource(Resource hostRes);
	}
}

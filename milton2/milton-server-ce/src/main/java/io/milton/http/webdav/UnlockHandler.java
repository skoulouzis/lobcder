/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.milton.http.webdav;

import io.milton.http.Handler;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.ResourceHandler;
import io.milton.http.ResourceHandlerHelper;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;
import io.milton.resource.Resource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * code based on sources in : http://svn.ettrema.com/svn/milton/tags/milton-1.7.2-SNAPSHOT/milton-api/
 */
class UnlockHandler implements ResourceHandler {

	private final ResourceHandlerHelper resourceHandlerHelper;
	private final WebDavResponseHandler responseHandler;

	public UnlockHandler(ResourceHandlerHelper resourceHandlerHelper, WebDavResponseHandler responseHandler) {
		this.resourceHandlerHelper = resourceHandlerHelper;
		this.responseHandler = responseHandler;
	}

	@Override
	public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException {
//		resourceHandlerHelper.process(httpManager, request, response, this);

		String host = request.getHostHeader();
		String url = HttpManager.decodeUrl(request.getAbsolutePath());

		// Find a resource if it exists
		Resource r = httpManager.getResourceFactory().getResource(host, url);
		if (r != null) {
			processExistingResource(httpManager, request, response, r);
		} else {
//            log.debug( "lock target doesnt exist, attempting lock null.." );
//            processNonExistingResource( httpManager, request, response, host, url );
		}
	}

	@Override
	public void processResource(HttpManager manager, Request request, Response response, Resource r) throws NotAuthorizedException, ConflictException, BadRequestException {
//		resourceHandlerHelper.process(manager, request, response, this);
		// Find a resource if it exists

		if (r != null) {
			processExistingResource(manager, request, response, r);
		} else {
//            log.debug( "lock target doesnt exist, attempting lock null.." );
//            processNonExistingResource( httpManager, request, response, host, url );
		}
	}

	private void processExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException, ConflictException {
		LockableResource r = (LockableResource) resource;
		String sToken = request.getLockTokenHeader();
		sToken = parseToken(sToken);

		// this should be checked in processResource now
//       	if( r.getCurrentLock() != null &&
//       			!sToken.equals( r.getCurrentLock().tokenId))
//    	{
		//Should this be unlocked easily? With other tokens?
//    		response.setStatus(Status.SC_LOCKED);
//    	    log.info("cant unlock with token: " + sToken);
//    		return;
//    	}
		try {
			r.unlock(sToken);
			responseHandler.respondNoContent(resource, response, request);
		} catch (PreConditionFailedException ex) {
			responseHandler.respondPreconditionFailed(request, response, resource);
		}
	}

	@Override
	public String[] getMethods() {
		return new String[]{Method.UNLOCK.code};
	}

	@Override
	public boolean isCompatible(Resource handler) {
		return handler instanceof LockableResource;
	}

	static String parseToken(String ifHeader) {
		String token = ifHeader;
		int pos = token.indexOf(":");
		if (pos >= 0) {
			token = token.substring(pos + 1);
			pos = token.indexOf(">");
			if (pos >= 0) {
				token = token.substring(0, pos);
			}
		}
		return token;
	}
}

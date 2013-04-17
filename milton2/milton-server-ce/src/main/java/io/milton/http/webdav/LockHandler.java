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

import io.milton.common.Utils;
import io.milton.http.Handler;
import io.milton.http.HandlerHelper;
import io.milton.http.HttpManager;
import io.milton.http.LockInfo;
import io.milton.http.LockInfo.LockDepth;
import io.milton.http.LockInfo.LockScope;
import io.milton.http.LockInfo.LockType;
import io.milton.http.LockInfoSaxHandler;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.Response;
import io.milton.http.Response.Status;
import io.milton.http.XmlWriter;
import io.milton.http.entity.ByteArrayEntity;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.LockableResource;
import io.milton.resource.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * code based on sources in : http://svn.ettrema.com/svn/milton/tags/milton-1.7.2-SNAPSHOT/milton-api/
 */
class LockHandler implements Handler {

	private final LockWriterHelper lockWriterHelper;

	private static class LockWriterHelper {

		private static String DAV = WebDavProtocol.DAV_PREFIX;
		private boolean stripHrefOnOwner = true;

		private void appendType(XmlWriter writer, LockType lockType) {
			writer.writeProperty(null, DAV + ":locktype", "<" + DAV + ":" + lockType.toString().toLowerCase() + "/>");
		}

		private void appendScope(XmlWriter writer, LockScope lockScope) {
			writer.writeProperty(null, DAV + ":lockscope", "<" + DAV + ":" + lockScope.toString().toLowerCase() + "/>");
		}

		private void appendDepth(XmlWriter writer, LockDepth lockDepth) {
			String s = "Infinity";
			if (lockDepth != null) {
				if (lockDepth.equals(LockInfo.LockDepth.INFINITY)) {
					s = lockDepth.name().toUpperCase();
				}
			}
			writer.writeProperty(null, DAV + ":depth", s);
		}

		private void appendOwner(XmlWriter writer, String lockedByUser) {
			boolean validHref;
			if (lockedByUser == null) {

				validHref = false;
			} else {
				validHref = isValidHref(lockedByUser);
			}
//        log.debug( "appendOwner: " + validHref + " - " + stripHrefOnOwner);
			if (!validHref && stripHrefOnOwner) { // BM: reversed login on validHref - presumably only write href tag for href values???
				writer.writeProperty(null, DAV + ":owner", lockedByUser);
			} else {
				XmlWriter.Element el = writer.begin(DAV + ":owner").open();
				XmlWriter.Element el2 = writer.begin(DAV + ":href").open();
				if (lockedByUser != null) {
					el2.writeText(lockedByUser);
				}
				el2.close();
				el.close();
			}
		}

		private boolean isValidHref(String owner) {
			if (owner.startsWith("http")) {
				try {
					URI u = new URI(owner);
					return true;
				} catch (URISyntaxException ex) {
					return false;
				}
			} else {
				return false;
			}
		}

		private void appendTimeout(XmlWriter writer, Long seconds) {
			if (seconds != null && seconds > 0) {
				writer.writeProperty(null, DAV + ":timeout", "Second-" + Utils.withMax(seconds, 4294967295l));
			}
		}

		private void appendTokenId(XmlWriter writer, String tokenId) {
			XmlWriter.Element el = writer.begin(DAV + ":locktoken").open();
			writer.writeProperty(null, DAV + ":href", "opaquelocktoken:" + tokenId);
			el.close();
		}

		private void appendRoot(XmlWriter writer, String url) {
			XmlWriter.Element el = writer.begin(DAV + ":lockroot").open();
			writer.writeProperty(null, DAV + ":href", url);
			el.close();
		}
	}
	private final WebDavResponseHandler responseHandler;
	private final HandlerHelper handlerHelper;

	public LockHandler(WebDavResponseHandler responseHandler, HandlerHelper handlerHelper) {
		this.responseHandler = responseHandler;
		this.handlerHelper = handlerHelper;
		lockWriterHelper = new LockWriterHelper();
	}

	@Override
	public String[] getMethods() {
		return new String[]{Method.LOCK.code};
	}

	@Override
	public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException, NotFoundException {
		if (!handlerHelper.checkExpects(responseHandler, request, response)) {
			return;
		}

		String host = request.getHostHeader();
		String url = HttpManager.decodeUrl(request.getAbsolutePath());
		Resource r = httpManager.getResourceFactory().getResource(host, url);
		try {
			// Find a resource if it exists
			if (r != null) {
				processExistingResource(httpManager, request, response, r);

			} else {
//            processNonExistingResource( manager, request, response, host, url );
			}
		} catch (IOException ex) {
			throw new BadRequestException(r);
		} catch (SAXException ex) {
			throw new BadRequestException(r);
		} catch (LockedException ex) {
			responseHandler.respondLocked(request, response, r);
		} catch (PreConditionFailedException ex) {
			responseHandler.respondPreconditionFailed(request, response, r);
		}
	}

	private void processExistingResource(HttpManager manager, Request request, Response response, Resource resource) throws NotAuthorizedException, IOException, SAXException, LockedException, PreConditionFailedException {
		if (handlerHelper.isNotCompatible(resource, request.getMethod()) || !isCompatible(resource)) {
			responseHandler.respondMethodNotImplemented(resource, response, request);
			return;
		}
		if (!handlerHelper.checkAuthorisation(manager, resource, request)) {
			responseHandler.respondUnauthorised(resource, response, request);
			return;
		}

		handlerHelper.checkExpects(responseHandler, request, response);

		LockableResource r = (LockableResource) resource;
		LockTimeout timeout = LockTimeout.parseTimeout(request);
		String ifHeader = request.getIfHeader();
		response.setContentTypeHeader(Response.XML);
		if (ifHeader == null || ifHeader.length() == 0) {
			processNewLock(manager, request, response, r, timeout);
		} else {
//            processRefresh(manager, request, response, r, timeout, ifHeader);
		}
	}

	private void processNewLock(HttpManager manager, Request request, Response response, LockableResource r, LockTimeout timeout) throws IOException, SAXException, NotAuthorizedException, PreConditionFailedException, LockedException {
		LockInfo lockInfo = parseLockInfo(request);

		if (handlerHelper.isLockedOut(request, r)) {
			this.responseHandler.respondLocked(request, response, r);
			return;
		}


		LockResult result = r.lock(timeout, lockInfo);

		if (result.isSuccessful()) {
			LockToken tok = result.getLockToken();
			response.setLockTokenHeader("<opaquelocktoken:" + tok.tokenId + ">");  // spec says to set response header. See 8.10.1
			respondWithToken(tok, request, response);
		} else {
			response.setStatus(result.getFailureReason().status);
		}
	}

	private void respondWithToken(LockToken tok, Request request, Response response) {
		response.setStatus(Status.SC_OK);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmlWriter writer = new XmlWriter(out);
		writer.writeXMLHeader();
		String d = WebDavProtocol.DAV_PREFIX;
		writer.open(d + ":prop  xmlns:" + d + "=\"DAV:\"");
		writer.newLine();
		writer.open(d + ":lockdiscovery");
		writer.newLine();
		writer.open(d + ":activelock");
		writer.newLine();
		lockWriterHelper.appendType(writer, tok.info.type);
		lockWriterHelper.appendScope(writer, tok.info.scope);
		lockWriterHelper.appendDepth(writer, tok.info.depth);
		lockWriterHelper.appendOwner(writer, tok.info.lockedByUser);
		lockWriterHelper.appendTimeout(writer, tok.timeout.getSeconds());
		lockWriterHelper.appendTokenId(writer, tok.tokenId);

		String url = request.getAbsoluteUrl().replace("&", "%26");
		lockWriterHelper.appendRoot(writer, url);
		writer.close(d + ":activelock");
		writer.close(d + ":lockdiscovery");
		writer.close(d + ":prop");
		writer.flush();
		response.setEntity(new ByteArrayEntity(out.toByteArray()));
	}

	private LockInfo parseLockInfo(Request request) throws IOException, SAXException {
		InputStream in = request.getInputStream();
		XMLReader reader = XMLReaderFactory.createXMLReader();
		LockInfoSaxHandler handler = new LockInfoSaxHandler();
		reader.setContentHandler(handler);
		reader.parse(new InputSource(in));
		LockInfo info = handler.getInfo();
		info.depth = LockDepth.INFINITY; // todo
		info.lockedByUser = null;
		if (request.getAuthorization() != null) {
			info.lockedByUser = request.getAuthorization().getUser();
		}
		if (info.lockedByUser == null) {
		}

		return info;
	}

	@Override
	public boolean isCompatible(Resource res) {
		return res instanceof LockableResource;
	}
}

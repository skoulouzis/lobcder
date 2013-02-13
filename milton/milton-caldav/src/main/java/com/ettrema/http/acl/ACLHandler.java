package com.ettrema.http.acl;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.DefaultPropPatchParser;
import com.bradmcevoy.http.webdav.PropPatchRequestParser;
import com.bradmcevoy.http.webdav.PropPatchRequestParser.ParseResult;
import com.bradmcevoy.http.webdav.PropPatchSaxHandler;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.StreamUtils;
import com.bradmcevoy.io.WritingException;
import com.ettrema.http.AccessControlledResource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author brad
 */
public class ACLHandler implements Handler {

    private Logger log = LoggerFactory.getLogger(ACLHandler.class);
    private final WebDavResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ACLRequestParser requestParser;

    public ACLHandler(WebDavResponseHandler responseHandler, HandlerHelper handlerHelper) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        requestParser = new ACLRequestParser();
    }

    @Override
    public String[] getMethods() {
        return new String[]{Method.ACL.code};
    }

    @Override
    public void process(HttpManager httpManager, Request request, Response response) throws ConflictException, NotAuthorizedException, BadRequestException {
//        response.setStatus( Response.Status.SC_OK );
        if (!handlerHelper.checkExpects(responseHandler, request, response)) {
            return;
        }

        String host = request.getHostHeader();
        String url = HttpManager.decodeUrl(request.getAbsolutePath());

        // Find a resource if it exists
        Resource r = httpManager.getResourceFactory().getResource(host, url);
        if (r != null) {
            log.debug("locking existing resource: " + r.getName());
            processExistingResource(httpManager, request, response, r);
        } else {
            log.debug("lock target doesnt exist, attempting lock null..");
            processNonExistingResource(httpManager, request, response, host, url);
        }
    }

    @Override
    public boolean isCompatible(Resource res) {
        return (res instanceof AccessControlledResource);
    }

    private void processExistingResource(HttpManager httpManager, Request request, Response response, Resource r) {
        InputStream in = null;
        try {
            if (handlerHelper.isNotCompatible(r, request.getMethod()) || !isCompatible(r)) {
                responseHandler.respondMethodNotImplemented(r, response, request);
                return;
            }
            if (!handlerHelper.checkAuthorisation(httpManager, r, request)) {
                responseHandler.respondUnauthorised(r, response, request);
                return;
            }
            handlerHelper.checkExpects(responseHandler, request, response);
            AccessControlledResource resource = (AccessControlledResource) r;
            Map<String, String> headers = request.getHeaders();
            Set<String> keys = headers.keySet();
            for (String k : keys) {
                System.err.println(k + " : " + headers.get(keys));
            }
            in = request.getInputStream();
            ParseResult res = requestParser.getRequestedFields(in);

            response.setContentTypeHeader(Response.XML);
            response.setStatus(Response.Status.SC_OK);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ACLHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ACLHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void processNonExistingResource(HttpManager httpManager, Request request, Response response, String host, String url) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static class ACLRequestParser {

        public ParseResult getRequestedFields(InputStream in) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                StreamUtils.readTo(in, bout, false, true);
                byte[] arr = bout.toByteArray();
                return parseContent(arr);
//                return parseContent(in);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private ParseResult parseContent(byte[] arr) throws IOException, SAXException {
            if (arr.length > 0) {
                ByteArrayInputStream bin = new ByteArrayInputStream(arr);
                XMLReader reader = XMLReaderFactory.createXMLReader();
                DefaultHandler handler = new DefaultHandler();
                reader.setContentHandler(handler);
                reader.parse(new InputSource(bin));
                return new ParseResult(null, null);
            } else {
                return new ParseResult(new HashMap<QName, String>(), new HashSet<QName>());
            }
        }

        private ParseResult parseContent(InputStream in) throws XMLStreamException {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            while (reader.hasNext()) {

                System.err.println("name: " + reader.getLocalName() + " getElementText: " + reader.getElementText() + " text: " + reader.getText() + " getName: " + reader.getName());

//                System.err.println(reader.getText());
                reader.next();
            }

            return null;
        }
    }

    private static class ACLSaxHandler extends DefaultHandler {

        private final static Logger log = LoggerFactory.getLogger(PropPatchSaxHandler.class);
        private Stack<String> elementPath = new Stack<String>();
        private Map<QName, String> attributesCurrent; // will switch between the following
        private Map<QName, String> attributesSet = new LinkedHashMap<QName, String>();
        private Map<QName, String> attributesRemove = new LinkedHashMap<QName, String>();
        private StringBuilder sb = new StringBuilder();
        private boolean inProp;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (inProp) {
                sb.append("<" + localName + ">");
            }
            for (String s : elementPath) {
                System.err.println(s);
            }
            if (elementPath.size() > 0) {
                if (attributesCurrent != null) {
                    if (elementPath.peek().endsWith("prop")) {
                        inProp = true;
                    }
                } else {
                    if (elementPath.peek().endsWith("set")) {
                        attributesCurrent = attributesSet;
                    }
                    if (elementPath.peek().endsWith("remove")) {
                        attributesCurrent = attributesRemove;
                    }
                }

            }
            elementPath.push(localName);
            super.startElement(uri, localName, name, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inProp) {
                sb.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            elementPath.pop();

            for (String s : elementPath) {
                System.err.println(s);
            }

            if (elementPath.size() > 0) {
                if (elementPath.peek().endsWith("prop")) {
                    if (sb != null) {
                        String s = sb.toString().trim();
                        QName qname = new QName(uri, localName);
                        attributesCurrent.put(qname, s);
                    }
                    sb = new StringBuilder();
                } else {
                    if (inProp) {
                        sb.append("</" + localName + ">");
                    }

                    if (elementPath.peek().endsWith("set")) {
                        attributesCurrent = null;
                    } else if (elementPath.peek().endsWith("remove")) {
                        attributesCurrent = null;
                    }
                }

            }

            super.endElement(uri, localName, name);
        }

        public Map<QName, String> getAttributesToSet() {
            return attributesSet;
        }

        public Map<QName, String> getAttributesToRemove() {
            return attributesRemove;
        }
    }
}

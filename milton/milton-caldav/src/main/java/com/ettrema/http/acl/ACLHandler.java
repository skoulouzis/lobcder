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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
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
//                ByteArrayOutputStream bout = new ByteArrayOutputStream();
//                StreamUtils.readTo(in, bout, false, true);
//                byte[] arr = bout.toByteArray();
//                return parseContent(arr);
                return parseContent(in);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private ParseResult parseContent(InputStream in) throws XMLStreamException {
//            XMLInputFactory factory = XMLInputFactory.newInstance();
//            XMLEventReader reader = factory.createXMLEventReader(in);

//            while (reader.hasNext()) {
//                XMLEvent event = reader.nextEvent();
//                System.err.println(event);
//            }

            ACL test = JAXB.unmarshal(in, ACL.class);
            Iterator<ACE> iter = test.ace.iterator();
            while (iter.hasNext()) {
                ACE pr = iter.next();
                if (pr.principal != null) {
                    System.err.println("principal: " + pr.principal);
                    System.err.println("principal.href: \t" + pr.principal.href);
                }

                if (pr.grant != null) {
                    System.err.println("grant: " + pr.grant);

//                    System.err.println("grant.privilege: \t" + pr.grant.privilege);
                    Iterator<Privilege> ii = pr.grant.privilege.iterator();
                    while (ii.hasNext()) {
                        Privilege privi = ii.next();
                        System.err.println("grant.privilege.read: \t\t" + privi.read);
                        System.err.println("grant.privilege.write: \t\t" + privi.write);
                    }
                }

                if (pr.deny != null) {
//                    System.err.println("deny: " + pr.deny);

//                    System.err.println("deny.privilege: \t" + pr.deny.privilege);
                    Iterator<Privilege> ii = pr.deny.privilege.iterator();
                    while (ii.hasNext()) {
                        Privilege privi = ii.next();
                        System.err.println("deny.privilege.read: \t\t" + privi.read);
                        System.err.println("deny.privilege.write: \t\t" + privi.write);
                    }
                }


            }

            return new ParseResult(null, null);
        }
    }

    @XmlRootElement(namespace = "DAV:")
    public static class ACL {

        @XmlElement(name = "ace", namespace = "DAV:")
        public Set<ACE> ace;
    }

    private static class ACE {

        @XmlElement(name = "principal", namespace = "DAV:")
        public Principal principal;
        @XmlElement(name = "grant", namespace = "DAV:")
        public Grant grant;
        @XmlElement(name = "deny", namespace = "DAV:")
        public Deny deny;
    }

    private static class Principal {

        @XmlElement(name = "href", namespace = "DAV:")
        public String href;
    }

    private static class Grant {

        @XmlElement(name = "privilege", namespace = "DAV:")
        public Set<Privilege> privilege;
    }

    private static class Deny {

        @XmlElement(name = "privilege", namespace = "DAV:")
        public Set<Privilege> privilege;
    }

    private static class Privilege {

        @XmlElement(name = "read", namespace = "DAV:")
        public String read;
        @XmlElement(name = "write", namespace = "DAV:")
        public String write;
    }
}

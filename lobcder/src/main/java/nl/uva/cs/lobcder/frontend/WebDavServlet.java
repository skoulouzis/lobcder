/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.http11.Http11Protocol;
import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.ettrema.http.caldav.CalDavProtocol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactory;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactoryFactory;

/**
 *
 * @author S. Koulouzis
 */
public class WebDavServlet implements Servlet {

    private static final ThreadLocal<HttpServletRequest> originalRequest = new ThreadLocal<HttpServletRequest>();
    private static final ThreadLocal<HttpServletResponse> originalResponse = new ThreadLocal<HttpServletResponse>();
    protected com.bradmcevoy.http.ServletHttpManager servletHttpManager;
    private ServletConfig config;
//    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final boolean debug = false;
    private ResourceFactory rf;
    private JDBCatalogue catalogue = null;

    public static HttpServletRequest request() {
        return originalRequest.get();
    }

    public static void setThreadlocals(HttpServletRequest req, HttpServletResponse resp) {
        originalRequest.set(req);
        originalResponse.set(resp);
    }
    private HttpManager httpManager;

    @Override
    public void service(javax.servlet.ServletRequest servletRequest,
            javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        //Enforce Basic auth 
        if (req.getHeader("Authorization") == null) {
            resp.setHeader("WWW-Authenticate", "Basic realm=\"SECRET\"");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {

            debug("HttpServletRequest \n"
                    + "\t getAuthType: " + req.getAuthType() + "\n"
                    + "\t getContentType: " + req.getContentType() + "\n"
                    + "\t getContextPath: " + req.getContextPath() + "\n"
                    + "\t getMethod: " + req.getMethod() + "\n"
                    + "\t getPathInfo: " + req.getPathInfo() + "\n"
                    + "\t getPathTranslated: " + req.getPathTranslated() + "\n"
                    + "\t getQueryString: " + req.getQueryString() + "\n"
                    + "\t getRemoteUser: " + req.getRemoteUser() + "\n"
                    + "\t getRequestURI: " + req.getRequestURI() + "\n"
                    + "\t getRequestedSessionId: " + req.getRequestedSessionId());
            //            Collection<Handler> handlers = servletHttpManager.getAllHandlers();
            //            for (Handler h : handlers) {
            //                debug("servletHttpManager Handler: " + h.getClass().getName());
            //            }

            try {
                originalRequest.set(req);
                originalResponse.set(resp);
                com.bradmcevoy.http.Request request = new com.bradmcevoy.http.ServletRequest(req);
                com.bradmcevoy.http.Response response = new com.bradmcevoy.http.ServletResponse(resp);
//                    servletHttpManager.process(request, response);
                httpManager.process(request, response);
            } finally {
                originalRequest.remove();
                originalResponse.remove();
                servletResponse.getOutputStream().flush();
                servletResponse.flushBuffer();
            }
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            this.config = config;
            String jndiName = "bean/JDBCatalog";
            Context ctx = new InitialContext();
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            Context envContext = (Context) ctx.lookup("java:/comp/env");
            catalogue = (JDBCatalogue) envContext.lookup(jndiName);
            rf = new WebDataResourceFactory(catalogue);
            catalogue.startSweep();

//            // Note that the config variable may be null, in which case default handlers will be used
//            // If present and blank, NO handlers will be configed
//            List<String> authHandlers = loadAuthHandlersIfAny(config.getInitParameter("authentication.handler.classes"));
//
//            initFromFactoryFactory(authHandlers);
//            servletHttpManager.init(new ApplicationConfig(config), servletHttpManager);


//            WebDavResourceTypeHelper rth = new com.bradmcevoy.http.webdav.WebDavResourceTypeHelper();
//        CalendarResourceTypeHelper crth = new com.ettrema.http.caldav.CalendarResourceTypeHelper(
//                new com.ettrema.http.acl.AccessControlledResourceTypeHelper(rth));


            AuthenticationService authService = new com.bradmcevoy.http.AuthenticationService();
            HandlerHelper hh = new com.bradmcevoy.http.HandlerHelper(authService);
            DefaultWebDavResponseHandler defaultResponseHandler = new com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler(authService);
            Http11Protocol http11 = new com.bradmcevoy.http.http11.Http11Protocol(defaultResponseHandler, hh);

            WebDavProtocol webdav = new com.bradmcevoy.http.webdav.WebDavProtocol(defaultResponseHandler, hh);

            CalDavProtocol caldav = new com.ettrema.http.caldav.CalDavProtocol(rf, defaultResponseHandler, hh, webdav);

//            ACLProtocol acl = new com.ettrema.http.acl.ACLProtocol(webdav);
            MyACLProtocol acl = new MyACLProtocol(webdav);
            ProtocolHandlers protocolHandlers = new com.bradmcevoy.http.ProtocolHandlers(Arrays.asList(http11, webdav, acl, caldav));

            httpManager = new com.bradmcevoy.http.HttpManager(rf, defaultResponseHandler, protocolHandlers);

            Collection<Handler> handlers = httpManager.getAllHandlers();
            for (Handler h : handlers) {
                debug("httpManager Handler: " + h.getClass().getName());
            }

        } catch (Exception ex) {
            debug("Exception starting WebDavServlet servlet " + ex);
            throw new RuntimeException(ex);
        }
    }

    protected void init(String responseHandlerClassName, List<String> authHandlers) throws Exception {
        rf = new WebDataResourceFactory(null);
        WebDavResponseHandler responseHandler;
        if (responseHandlerClassName == null) {
            responseHandler = null; // allow default to be created
        } else {
            responseHandler = instantiate(responseHandlerClassName);
        }
        init(rf, responseHandler, authHandlers);
    }

    protected void init(ResourceFactory rf, WebDavResponseHandler responseHandler, List<String> authHandlers) throws ServletException {
        AuthenticationService authService;
        List<com.bradmcevoy.http.AuthenticationHandler> list = new ArrayList<com.bradmcevoy.http.AuthenticationHandler>();
        for (String authHandlerClassName : authHandlers) {
            Object o = instantiate(authHandlerClassName);
            if (o instanceof com.bradmcevoy.http.AuthenticationHandler) {
                com.bradmcevoy.http.AuthenticationHandler auth = (com.bradmcevoy.http.AuthenticationHandler) o;
                list.add(auth);
            } else {
                throw new ServletException("Class: " + authHandlerClassName + " is not a: " + com.bradmcevoy.http.AuthenticationHandler.class.getCanonicalName());
            }
        }

        authService = new AuthenticationService(list);
        authService.setDisableDigest(true);
        authService.setDisableBasic(false);
        authService.setDisableExternal(true);

        // log the auth handler config
        debug("Configured authentication handlers: " + authService.getAuthenticationHandlers().size());
        if (authService.getAuthenticationHandlers().size() > 0) {
            for (com.bradmcevoy.http.AuthenticationHandler hnd : authService.getAuthenticationHandlers()) {
                debug("AuthenticationHandler - " + hnd.getClass().getCanonicalName());
            }
        } else {
            debug("No authentication handlers are configured! Any requests requiring authorisation will fail.");
        }

//        WebDavProtocol webdav = new com.bradmcevoy.http.webdav.WebDavProtocol(responseHandler, null);
//        com.ettrema.http.acl.ACLProtocol acl = new com.ettrema.http.acl.ACLProtocol(webdav);
//        List protocolList = Arrays.asList(webdav, acl, authService);
//        ProtocolHandlers protocols = new com.bradmcevoy.http.ProtocolHandlers(protocolList);
//        HttpManager httpManager = new com.bradmcevoy.http.HttpManager(rf, responseHandler, protocols);


        servletHttpManager = new com.bradmcevoy.http.ServletHttpManager(rf, responseHandler, authService);
        servletHttpManager.addFilter(0, new WebDavFilter());

    }

    protected <T> T instantiate(String className) throws ServletException {
        try {
            Class c = Class.forName(className);
            T rf = (T) c.newInstance();
            return rf;
        } catch (Throwable ex) {
            throw new ServletException("Failed to instantiate: " + className, ex);
        }
    }

    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    @Override
    public String getServletInfo() {
        return this.getClass().getName();
    }

    @Override
    public void destroy() {
        debug("destroy");
        if (catalogue != null) {
            catalogue.stopSweep();
        }
        if (servletHttpManager == null) {
            return;
        }
        servletHttpManager.destroy(servletHttpManager);
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }
    }

    /**
     * Returns null, or a list of configured authentication handler class names
     *
     * @param initParameter - null, or the (possibly empty) list of comma
     * seperated class names
     * @return - null, or a possibly empty list of class names
     */
    private List<String> loadAuthHandlersIfAny(String initParameter) {
        if (initParameter == null) {
            return null;
        }
        String[] arr = initParameter.split(",");
        List<String> list = new ArrayList<String>();
        for (String s : arr) {
            s = s.trim();
            if (s.length() > 0) {
                list.add(s);
            }
        }
        return list;
    }

    protected void initFromFactoryFactory(List<String> authHandlers) throws ServletException {
        com.bradmcevoy.http.ResourceFactoryFactory rff = new WebDataResourceFactoryFactory();
        rff.init();
        rf = rff.createResourceFactory();
        WebDavResponseHandler responseHandler = rff.createResponseHandler();
        init(rf, responseHandler, authHandlers);
    }
}

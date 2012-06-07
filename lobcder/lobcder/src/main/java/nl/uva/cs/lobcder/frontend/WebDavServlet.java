/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import com.bradmcevoy.http.ApplicationConfig;
import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.MiltonServlet;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactory;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactoryFactory;

/**
 *
 * @author S. Koulouzis
 */
public class WebDavServlet implements Servlet {

    private static final ThreadLocal<HttpServletRequest> originalRequest = new ThreadLocal<HttpServletRequest>();
    private static final ThreadLocal<HttpServletResponse> originalResponse = new ThreadLocal<HttpServletResponse>();
    protected com.bradmcevoy.http.ServletHttpManager httpManager;
    private ServletConfig config;
//    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final boolean debug = true;
    private ResourceFactory rf;

    public static HttpServletRequest request() {
        return originalRequest.get();
    }

    public static void setThreadlocals(HttpServletRequest req, HttpServletResponse resp) {
        originalRequest.set(req);
        originalResponse.set(resp);
    }

    @Override
    public void service(javax.servlet.ServletRequest servletRequest,
            javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        final String auth = req.getHeader("Authorization");
        if (auth == null) {
            resp.setHeader("WWW-Authenticate", "Basic realm=\"" + "SECRET" + "\"");
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

            try {
                originalRequest.set(req);
                originalResponse.set(resp);

                com.bradmcevoy.http.Request request = new com.bradmcevoy.http.ServletRequest(req);
                com.bradmcevoy.http.Response response = new com.bradmcevoy.http.ServletResponse(resp);

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
            // Note that the config variable may be null, in which case default handlers will be used
            // If present and blank, NO handlers will be configed
            List<String> authHandlers = loadAuthHandlersIfAny(config.getInitParameter("authentication.handler.classes"));

            initFromFactoryFactory(authHandlers);

            httpManager.init(new ApplicationConfig(config), httpManager);
        } catch (Exception ex) {
            debug("Exception starting WebDavServlet servlet " + ex);
            throw new RuntimeException(ex);
        }
    }

    protected void init(String responseHandlerClassName, List<String> authHandlers) throws Exception {
        rf = new WebDataResourceFactory();

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

        if (authHandlers == null) {
            authService = new AuthenticationService();
        } else {
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
        }

        authService = new AuthenticationService();
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

        if (responseHandler == null) {
            httpManager = new com.bradmcevoy.http.ServletHttpManager(rf, authService);
        } else {
            httpManager = new com.bradmcevoy.http.ServletHttpManager(rf, responseHandler, authService);
        }

        httpManager.addFilter(0, new WebDavFilter());
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
        if (httpManager == null) {
            return;
        }
        httpManager.destroy(httpManager);
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import com.bradmcevoy.http.ApplicationConfig;
import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.uva.vlet.exception.VlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nl.uva.cs.lobcder.webDav.resources.DataResourceFactory;
import nl.uva.cs.lobcder.webDav.resources.DataResourceFactoryFactory;

/**
 *
 * @author S. Koulouzis
 */
public class WebDavServlet implements Servlet {

    private static final ThreadLocal<HttpServletRequest> originalRequest = new ThreadLocal<HttpServletRequest>();
    private static final ThreadLocal<HttpServletResponse> originalResponse = new ThreadLocal<HttpServletResponse>();
    protected com.bradmcevoy.http.ServletHttpManager httpManager;
    private ServletConfig config;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final boolean debug = false;

    @Override
    public void service(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        debug("Mehod: " + req.getMethod() + " AuthType:" + req.getAuthType() + " ContextPath:" + req.getContextPath() + " PathInfo:" + req.getPathInfo());


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


    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            this.config = config;
            // Note that the config variable may be null, in which case default handlers will be used
            // If present and blank, NO handlers will be configed
            List<String> authHandlers = loadAuthHandlersIfAny(config.getInitParameter("authentication.handler.classes"));

            initFromFactoryFactory(authHandlers);
//            if (resourceFactoryFactoryClassName != null && resourceFactoryFactoryClassName.length() > 0) {
//                
//            } else {
//                
//                String responseHandlerClassName = config.getInitParameter("response.handler.class");
//                
//                debug("responseHandlerClassName: " + responseHandlerClassName);
//                init( responseHandlerClassName, authHandlers);
//            }
            httpManager.init(new ApplicationConfig(config), httpManager);
        } catch (Exception ex) {
            debug("Exception starting WebDavServlet servlet " + ex);
            throw new RuntimeException(ex);
        }
    }

    protected void init(String responseHandlerClassName, List<String> authHandlers) throws ServletException, URISyntaxException, VlException, IOException {

        ResourceFactory rf = new DataResourceFactory();

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
        if(debug){
        System.err.println(this.getClass().getSimpleName() + ": " + msg);
        log.debug(msg);
        }
    }

    /**
     * Returns null, or a list of configured authentication handler class names
     *
     * @param initParameter - null, or the (possibly empty) list of comma seperated class names
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
        com.bradmcevoy.http.ResourceFactoryFactory rff = new DataResourceFactoryFactory();
        rff.init();
        ResourceFactory rf = rff.createResourceFactory();
        WebDavResponseHandler responseHandler = rff.createResponseHandler();
        init(rf, responseHandler, authHandlers);
    }
}

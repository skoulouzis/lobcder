/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 *
 * @author S. Koulouzis
 */
public class SimpleCORSFilter implements Filter {

    private Set<String> allowedOrigins;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        try {
            allowedOrigins = PropertiesHelper.getAllowedOrigins();
        } catch (IOException ex) {
            Logger.getLogger(SimpleCORSFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        if (((HttpServletRequest) request).getHeader("Access-Control-Request-Method") != null && "OPTIONS".equals(((HttpServletRequest) request).getMethod())) {
            String originHeader = ((HttpServletRequest) request).getHeader("Origin");

            if (allowedOrigins.contains(((HttpServletRequest) request).getHeader("Origin"))) {
                ((HttpServletResponse) response).addHeader("Access-Control-Allow-Origin", originHeader);
            }

            ((HttpServletResponse) response).addHeader("Access-Control-Allow-Methods", "GET, POST, HEAD, OPTIONS, PUT, PROPFIND, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, MKCOL");
            ((HttpServletResponse) response).addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, Authorization");
            
            ((HttpServletResponse) response).addHeader("Access-Control-Max-Age", "1800");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

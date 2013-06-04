package nl.uva.cs.lobcder.frontend;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.LocalDbAuth;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import org.apache.commons.codec.binary.Base64;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;

/**
 * A very simple Servlet Filter for HTTP Basic Auth.
 * 
 * @author Timo B. Huebel (me@tbh.name) (initial creation)
 */
@Log
public class BasicAuthFilter implements Filter {

    private String _realm;
    private AuthI auth;

    @Override
    public void destroy() {
        // Nothing to do.
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String autheader = httpRequest.getHeader("Authorization");
        if (autheader != null) {

            final int index = autheader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index).getBytes()), "UTF8");
//                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);
                MyPrincipal principal = auth.checkToken(token);
                //Try the local db 
                if (principal == null) {
                    LocalDbAuth authT = null;
                    try {
                        authT = new LocalDbAuth();
                    } catch (NamingException e) {
                        throw new ServletException(e);
                    }
                    principal = authT.checkToken(token);
                }
                if (principal != null) {
                    httpRequest.setAttribute("myprincipal", principal);
                    chain.doFilter(httpRequest, httpResponse);
                    return;
                }
            }
        }

        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + _realm + "\"");
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        _realm = "SECRET";
        try {
            String jndiName = "bean/auth";
            javax.naming.Context ctx = new InitialContext();
            javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
            auth = (AuthI) envContext.lookup(jndiName);
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}

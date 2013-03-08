package nl.uva.cs.lobcder.frontend;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;

//import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.MyAuthTest;
import nl.uva.cs.lobcder.auth.MyPrincipal;

import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.lang.StringUtils;

//import com.google.common.base.Charsets;
/**
 * A very simple Servlet Filter for HTTP Basic Auth. Only supports exactly one
 * user with a password. Please note, HTTP Basic Auth is not encrypted and hence
 * unsafe!
 * 
* @author Timo B. Huebel (me@tbh.name) (initial creation)
 */
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
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);
                MyPrincipal principal = auth.checkToken(token);
                //Try the local db 
                if (principal == null) {
                    MyAuthTest authT = new MyAuthTest();
                    principal = authT.checkToken(token);
                }
                if(principal != null) {
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
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
            auth = (AuthI) envContext.lookup(jndiName);
        } catch (Exception ex) {
            Logger.getLogger(BasicAuthFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

package nl.uva.cs.lobcder.frontend;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.AuthWorker;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.codec.binary.Base64;

/**
 * A very simple Servlet Filter for HTTP Basic Auth.
 *
 * @author Timo B. Huebel (me@tbh.name) (initial creation)
 */
@Log
public class BasicAuthFilter implements Filter {

    private String _realm;
    private List<AuthI> authList;
    private AuthWorker authWorker;

    public BasicAuthFilter() {
    }

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

                MyPrincipal principal = null;
                List<String> workers = PropertiesHelper.getWorkers();

//                if (workers != null && workers.size() > 0 && uname.startsWith("worker")) {
//                    String remoteHost = request.getRemoteHost();
//                    if (remoteHost.equals("localhost") || remoteHost.equals("127.0.0.1")) {
//                        remoteHost = "localhost";
//                    }
//                    boolean foundHim = false;
//                    for (String s : workers) {
//                        try {
//                            String workerHost = new URI(s).getHost();
//                            if (remoteHost.equals(workerHost)) {
//                                foundHim = true;
//                                break;
//                            }
//                        } catch (URISyntaxException ex) {
//                            Logger.getLogger(BasicAuthFilter.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                    if (!foundHim) {
//                        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + _realm + "\"");
//                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//                        return;
//                    }
//                }
//                for (AuthI a : authList) {
//                    principal = a.checkToken(token);
//                    if (principal != null) {
//                        break;
//                    }
//                }

//
                if (workers != null && workers.size() > 0 && uname.startsWith("worker-")) {
                    if (authWorker == null) {
                        for (AuthI a : authList) {
                            if (a instanceof AuthWorker) {
                                authWorker = (AuthWorker) a;
                                break;
                            }
                        }
                    }
                    for (String s : workers) {
                        try {
                            String workerHost = new URI(s).getHost();
                            String remoteHost = request.getRemoteHost();
                            if (remoteHost.equals("localhost") || remoteHost.equals("127.0.0.1")) {
//                                InetAddress.getLocalHost().getHostName();
                                remoteHost = "localhost";
                            }
                            if (remoteHost.equals(workerHost)) {

                                principal = authWorker.checkToken(token);
                                if (principal != null) {
                                    break;
                                }
                            }
                        } catch (URISyntaxException ex) {
                            principal = null;
                            Logger.getLogger(BasicAuthFilter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else {
                    for (AuthI a : authList) {
                        if (a instanceof AuthWorker) {
                            continue;
                        }
                        principal = a.checkToken(token);
                        if (principal != null) {
                            break;
                        }
                    }
                }

//                //Try the local db 
//                if (principal == null) {
//                    LocalDbAuth authT = null;
//                    try {
//                        authT = new LocalDbAuth();
//                    } catch (NamingException e) {
//                        throw new ServletException(e);
//                    }
//                    principal = authT.checkToken(token);
//                }
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
            AuthI auth = (AuthI) envContext.lookup(jndiName);
            authList = new ArrayList<>();
            authList.add(auth);



            jndiName = "bean/authWorker";
            auth = (AuthI) envContext.lookup(jndiName);
            authList.add(auth);


            jndiName = "bean/authDB";
            auth = (AuthI) envContext.lookup(jndiName);
            authList.add(auth);

        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}

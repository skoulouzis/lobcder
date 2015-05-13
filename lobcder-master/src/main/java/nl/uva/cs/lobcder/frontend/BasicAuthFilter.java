package nl.uva.cs.lobcder.frontend;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.*;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import nl.uva.cs.lobcder.util.SingletonesHelper;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import nl.uva.cs.lobcder.resources.VPDRI;

/**
 * A very simple Servlet Filter for HTTP Basic Auth.
 *
 * @author Timo B. Huebel (me@tbh.name) (initial creation)
 */
@Log
public class BasicAuthFilter implements Filter {

    private String _realm;
    private List<AuthI> authList;
    private AuthLobcderComponents authWorker;

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


//        log.log(Level.INFO, "Auth for rest. autheader: "+autheader);

        if (autheader != null) {

            final int index = autheader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(autheader.substring(index).getBytes()), "UTF8");
//                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");
                final String uname = credentials.substring(0, credentials.indexOf(":"));
                final String token = credentials.substring(credentials.indexOf(":") + 1);


                double start = System.currentTimeMillis();


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
                if (uname.equals(VPDRI.class.getName())) {
                    if (request.getRemoteHost().equals("localhost") || request.getRemoteHost().equals("127.0.0.1")) {
                        for (AuthI a : authList) {
                            if (a instanceof AuthLobcderComponents) {
                                authWorker = (AuthLobcderComponents) a;
                                break;
                            }
                        }
                        principal = authWorker.checkToken(uname, token);
                    }

                } else if (PropertiesHelper.doRedirectGets() && workers != null
                        && workers.size() > 0 && uname.startsWith("worker-")) {
                    if (authWorker == null) {
                        for (AuthI a : authList) {
                            log.log(Level.INFO, "Init AuthWorker");
                            if (a instanceof AuthLobcderComponents) {
                                authWorker = (AuthLobcderComponents) a;
                                break;
                            }
                        }
                    }
                    principal = authWorker.checkToken(uname, token);

//                    for (String s : workers) {
//                        try {
//                            String workerHost = new URI(s).getHost();
//                            String remoteHost = request.getRemoteHost();
//                            if (remoteHost.equals("localhost") || remoteHost.equals("127.0.0.1")) {
////                                InetAddress.getLocalHost().getHostName();
//                                remoteHost = "localhost";
//                            }
//                            if (remoteHost.equals(workerHost)) {
//
//                                principal = authWorker.checkToken(token);
//                                if (principal != null) {
//                                    break;
//                                }
//                            }
//                        } catch (URISyntaxException ex) {
//                            principal = null;
//                            Logger.getLogger(BasicAuthFilter.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
                } else {
                    for (AuthI a : authList) {
                        if (a instanceof AuthLobcderComponents) {
                            continue;
                        }
//                        if (!PropertiesHelper.doRemoteAuth()
//                                && a instanceof AuthTicket) {
//                            continue;
//                        }
                        principal = a.checkToken(uname, token);
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

                String method = ((HttpServletRequest) httpRequest).getMethod();
                StringBuffer reqURL = ((HttpServletRequest) httpRequest).getRequestURL();
                double elapsed = System.currentTimeMillis() - start;

                String userAgent = ((HttpServletRequest) httpRequest).getHeader("User-Agent");

                String from = ((HttpServletRequest) httpRequest).getRemoteAddr();
//        String user = ((HttpServletRequest) httpRequest).getRemoteUser();
                int contentLen = ((HttpServletRequest) httpRequest).getContentLength();
                String contentType = ((HttpServletRequest) httpRequest).getContentType();

//                String authorizationHeader = ((HttpServletRequest) httpRequest).getHeader("authorization");
//                String userNpasswd = "";
//                if (authorizationHeader != null) {
//                    userNpasswd = authorizationHeader.split("Basic ")[1];
//                }
                String queryString = ((HttpServletRequest) httpRequest).getQueryString();

                log.log(Level.INFO, "Req_Source: {0} Method: {1} Content_Len: {2} "
                        + "Content_Type: {3} Elapsed_Time: {4} sec EncodedUser: {5} "
                        + "UserAgent: {6} queryString: {7} reqURL: {8}",
                        new Object[]{from, method, contentLen, contentType, elapsed / 1000.0, getUserName((HttpServletRequest) httpRequest), userAgent, queryString, reqURL});

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
        authList = SingletonesHelper.getInstance().getAuth();
        authList.add(new AuthLobcderComponents());
    }

    private String getUserName(HttpServletRequest httpServletRequest) throws UnsupportedEncodingException {
        String authorizationHeader = httpServletRequest.getHeader("authorization");
        String userNpasswd = "";
        if (authorizationHeader != null) {
            final int index = authorizationHeader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(authorizationHeader.substring(index).getBytes()), "UTF8");
                String[] encodedToken = credentials.split(":");
                if (encodedToken.length > 1) {
                    String token = new String(Base64.decodeBase64(encodedToken[1]));
                    if (token.contains(";") && token.contains("uid=")) {
                        String uid = token.split(";")[0];
                        userNpasswd = uid.split("uid=")[1];
                    } else {
                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
                    }
                }
//                    if (userNpasswd == null || userNpasswd.length() < 1) {
//                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
//                    }

//                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");

//                final String token = credentials.substring(credentials.indexOf(":") + 1);
            }
        }
        return userNpasswd;
    }
}

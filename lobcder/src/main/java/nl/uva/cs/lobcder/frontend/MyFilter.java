/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import io.milton.common.Path;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.optimization.FileAccessPredictor;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.util.CatalogueHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter {

    private JDBCatalogue catalogue;
    private static FileAccessPredictor fap;
    private static LobState prevState;

    public MyFilter() throws Exception {
//        getFileAccessPredictor();
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        double start = System.currentTimeMillis();
        String method = ((HttpServletRequest) req).getMethod();
        StringBuffer reqURL = ((HttpServletRequest) req).getRequestURL();



//        long startPredict = System.currentTimeMillis();
//        predict(Request.Method.valueOf(method), reqURL.toString());
//        long elapsedPredict = System.currentTimeMillis() - startPredict;
//        log.log(Level.INFO, "elapsedPredict: {0}", elapsedPredict);

        super.doFilter(req, resp, fc);
        double elapsed = System.currentTimeMillis() - start;



        String from = ((HttpServletRequest) req).getRemoteAddr();
//        String user = ((HttpServletRequest) req).getRemoteUser();
        int contentLen = ((HttpServletRequest) req).getContentLength();
        String contentType = ((HttpServletRequest) req).getContentType();

        String authorizationHeader = ((HttpServletRequest) req).getHeader("authorization");
        String userNpasswd = "";
        if (authorizationHeader != null) {
            userNpasswd = authorizationHeader.split("Basic ")[1];
        }

        log.log(Level.INFO, "Req_Source: {0} Method: {1} Content_Len: {2} Content_Type: {3} Elapsed_Time: {4} sec EncodedUser: {5}", new Object[]{from, method, contentLen, contentType, elapsed / 1000.0, userNpasswd});
        try (Connection connection = getCatalogue().getConnection()) {
            recordEvent(connection, ((HttpServletRequest) req), elapsed);
        } catch (SQLException ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public JDBCatalogue getCatalogue() {
        if (catalogue == null) {
            String jndiName = "bean/JDBCatalog";
            javax.naming.Context ctx;
            try {
                ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
                catalogue = (JDBCatalogue) envContext.lookup(jndiName);
            } catch (Exception ex) {
                Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return catalogue;
    }

    private void recordEvent(Connection connection, HttpServletRequest httpServletRequest, double elapsed) throws SQLException, UnsupportedEncodingException {
        getCatalogue().recordRequest(connection, httpServletRequest, elapsed);
        connection.commit();
    }

    private FileAccessPredictor getFileAccessPredictor() throws Exception {
        if (fap == null) {
            String jndiName = "bean/Predictor";
            javax.naming.Context ctx;
            try {
                ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
                fap = (FileAccessPredictor) envContext.lookup(jndiName);
                fap.startGraphPopulation();
            } catch (Exception ex) {
                Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fap;
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            getFileAccessPredictor().stopGraphPopulation();
        } catch (Exception ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void predict(Method method, String reqURL) {
        String strPath = null;
        try {
            strPath = new URL(reqURL).getPath();
        } catch (MalformedURLException ex) {
            strPath = reqURL;
        }
        String[] parts = strPath.split("/lobcder/dav");
        String resource = null;
        if (parts != null && parts.length > 1) {
            resource = parts[1];
        } else {
            resource = strPath;
        }

        LobState currentState = new LobState(method, Path.path(resource).toString());
        LobState nextState = null;
        try {
            nextState = getFileAccessPredictor().predictNextState(currentState);
            log.log(Level.INFO, "nextFile: {0}", nextState.getID());
        } catch (Exception ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (prevState != null) {
            if (currentState.getID().equals(prevState.getID())) {
                log.log(Level.INFO, "Hit. currentState: {0} nextState: {1} prevState: {2}",
                        new Object[]{currentState.getID(), nextState.getID(), prevState.getID()});
            } else {
                log.log(Level.INFO, "Miss. currentState: {0} nextState: {1} prevState: {2}",
                        new Object[]{currentState.getID(), nextState.getID(), prevState.getID()});
            }
        }

        prevState = nextState;
    }
}

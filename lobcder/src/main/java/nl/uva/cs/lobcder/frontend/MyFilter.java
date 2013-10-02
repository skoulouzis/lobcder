/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import com.google.common.io.CharStreams;
import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.CatalogueHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter {

    private JDBCatalogue catalogue;

    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        double start = System.currentTimeMillis();
        super.doFilter(req, resp, fc);
        double elapsed = System.currentTimeMillis() - start;


        String method = ((HttpServletRequest) req).getMethod();
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

    private void recordEvent(Connection connection, HttpServletRequest httpServletRequest, double elapsed) throws SQLException {
        getCatalogue().recordRequest(connection, httpServletRequest, elapsed);
        connection.commit();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter {

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
        log.log(Level.INFO, "Req_Source: {0} Method: {1} Content_Len: {2} Content_Type: {3} Elapsed_Time: {4} sec", new Object[]{from, method, contentLen, contentType, elapsed / 1000.0});
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import javax.servlet.ServletException;

/**
 *
 * @author S. Koulouzis
 */
public class MyFilter extends MiltonFilter{
    
    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        super.doFilter(req, resp, fc);
    }
    
}

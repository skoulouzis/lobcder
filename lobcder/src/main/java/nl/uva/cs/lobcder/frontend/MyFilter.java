/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import javax.servlet.ServletException;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter{
    
    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        double start =System.currentTimeMillis();
        super.doFilter(req, resp, fc);
        double elapsed = System.currentTimeMillis() -start; 
//        method = req.get
   }
    
}

package nl.uva.cs.lobcder.webDav.resources;

import java.io.IOException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.ResourceFactoryFactory;
import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import java.net.URISyntaxException;
import nl.uva.vlet.exception.VlException;

public class WebDataResourceFactoryFactory implements ResourceFactoryFactory {

    private Logger log = LoggerFactory.getLogger(WebDataResourceFactoryFactory.class);
    private static AuthenticationService authenticationService;
    private static WebDataResourceFactory resourceFactory;
    private boolean debug = false;

    @Override
    public ResourceFactory createResourceFactory() {
        return resourceFactory;
    }

    @Override
    public WebDavResponseHandler createResponseHandler() {
        return new DefaultWebDavResponseHandler(authenticationService);
    }

    @Override
    public void init() {
        debug("init");
        if (authenticationService == null) {
            try {
                authenticationService = new AuthenticationService();
                resourceFactory = new WebDataResourceFactory();
            } catch (VlException ex) {
                java.util.logging.Logger.getLogger(WebDataResourceFactoryFactory.class.getName()).log(Level.SEVERE, null, ex);
            } catch (URISyntaxException ex) {
                java.util.logging.Logger.getLogger(WebDataResourceFactoryFactory.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(WebDataResourceFactoryFactory.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }

    }
}

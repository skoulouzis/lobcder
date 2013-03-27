package nl.uva.cs.lobcder.frontend;

import io.milton.servlet.DefaultMiltonConfigurator;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthRemote;
import nl.uva.cs.lobcder.auth.LocalDbAuth;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Level;

@Log
public class MyMiltonConfigurator extends DefaultMiltonConfigurator {

    private JDBCatalogue catalogue;
    private AuthRemote authRemote;
    private LocalDbAuth localDbAuth;
    private WebDataResourceFactory webDataResourceFactory;

    @Override
    protected void build() {
        builder.setEnableCompression(false);
        builder.setEnableOptionsAuth(true);
        builder.setEnableBasicAuth(true);
        super.build();
        try {
            Context ctx = new InitialContext();
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            Context envContext = (Context) ctx.lookup("java:/comp/env");
            catalogue = (JDBCatalogue) envContext.lookup("bean/JDBCatalog");
            catalogue.startSweep();
            authRemote = (AuthRemote) envContext.lookup("bean/auth");
            localDbAuth = new LocalDbAuth();
            webDataResourceFactory = (WebDataResourceFactory) builder.getMainResourceFactory();
            webDataResourceFactory.setCatalogue(catalogue);
            webDataResourceFactory.setAuth1(authRemote);
            webDataResourceFactory.setAuth2(localDbAuth);
        } catch (Exception e) {
            MyMiltonConfigurator.log.log(Level.SEVERE, null, e);
        }
    }
}

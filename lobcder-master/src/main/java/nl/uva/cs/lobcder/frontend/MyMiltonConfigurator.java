package nl.uva.cs.lobcder.frontend;

import io.milton.servlet.DefaultMiltonConfigurator;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.AuthLobcderComponents;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import nl.uva.cs.lobcder.util.SingletonesHelper;
import nl.uva.cs.lobcder.webDav.resources.WebDataResourceFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.vrs.VRS;

@Log
public class MyMiltonConfigurator extends DefaultMiltonConfigurator {

    private JDBCatalogue catalogue;
    private WebDataResourceFactory webDataResourceFactory;
    private AuthLobcderComponents workerAuth;

//    public MyMiltonConfigurator() {
    //        super();
    //        try {
    //            // Attempt to use Enterprise edition build if available
    //            Class builderClass = Class.forName("nl.uva.cs.webdav.HttpManagerBuilderExt");
    //            builder = (HttpManagerBuilder) builderClass.newInstance();
    //            log.log(Level.INFO, "Using enterprise builder: {0}", builder.getClass());
    //        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
    //            log.info("Couldnt instantiate enterprise builder, DAV level 2 and beyond features will not be available");
    //            builder = new HttpManagerBuilder();
    //        }
//            Class handlerHelper = Class.forName("io.milton.http.HandlerHelper");
//            if (handlerHelper != null) {
//                throw new RuntimeException("HandlerHelper is not an instance of io.milton.http.HandlerHelper");
//            }
//            builder = new HttpManagerBuilderExt();
//    }
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

//            workerAuth = (AuthLobcderComponents) envContext.lookup("bean/authWorker");
            List<String> workers = PropertiesHelper.getWorkers();
            if (workers != null && workers.size() > 0) {
                workerAuth = new AuthLobcderComponents();
            }


            webDataResourceFactory = (WebDataResourceFactory) builder.getMainResourceFactory();
            webDataResourceFactory.setCatalogue(catalogue);
            List<AuthI> authList = SingletonesHelper.getInstance().getAuth();

            //            if(workerAuth!=null){
//            authList.add(workerAuth);
//            }
            webDataResourceFactory.setAuthList(authList);
///            webDataResourceFactory.setAuth1(authRemote);
//            webDataResourceFactory.setAuth2(localDbAuth);
//            webDataResourceFactory.setAuth3(workerAuth);


//            loadOptimizers(envContext);
        } catch (Exception e) {
            MyMiltonConfigurator.log.log(Level.SEVERE, null, e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            catalogue.stopSweep();
            //        VRS.exit();
        } catch (InterruptedException ex) {
            Logger.getLogger(MyMiltonConfigurator.class.getName()).log(Level.WARNING, null, ex);
        }
    }
//    private void loadOptimizers(Context envContext) throws NamingException {
//        FileAccessPredictor fap = (FileAccessPredictor) envContext.lookup("bean/Predictor");
//        fap.startGraphPopulation();
//    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.optimization.LDClustering;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.optimization.MyTask;
import nl.uva.cs.lobcder.predictors.FirstSuccessor;
import nl.uva.cs.lobcder.predictors.LastSuccessor;
import nl.uva.cs.lobcder.predictors.MarkovPredictor;
import nl.uva.cs.lobcder.predictors.PredecessorPosition;
import nl.uva.cs.lobcder.predictors.Predictor;
import nl.uva.cs.lobcder.predictors.RecentPopularity;
import nl.uva.cs.lobcder.predictors.StableSuccessor;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.codec.binary.Base64;
import nl.uva.cs.lobcder.predictors.ClusterPredictor;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter {

    private JDBCatalogue catalogue;
    private static Predictor predictor;
    private static LobState prevState;
    private static final BlockingQueue queue = new ArrayBlockingQueue(3500);
    private static RequestEventRecorder recorder;
    private Timer recordertimer;
    private LobState prevPrediction;

    public MyFilter() throws Exception {
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        double start = System.currentTimeMillis();
        String method = ((HttpServletRequest) req).getMethod();
        StringBuffer reqURL = ((HttpServletRequest) req).getRequestURL();

        if (PropertiesHelper.doPrediction()) {
            try {
                long startPredict = System.currentTimeMillis();
                predict(Request.Method.valueOf(method), reqURL.toString());
                long elapsedPredict = System.currentTimeMillis() - startPredict;
                log.log(Level.INFO, "elapsedPredict: {0}", elapsedPredict);
            } catch (Exception ex) {
                Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        super.doFilter(req, resp, fc);



        double elapsed = System.currentTimeMillis() - start;

        String userAgent = ((HttpServletRequest) req).getHeader("User-Agent");

        String from = ((HttpServletRequest) req).getRemoteAddr();
        int contentLen = ((HttpServletRequest) req).getContentLength();
        String contentType = ((HttpServletRequest) req).getContentType();

        String authorizationHeader = ((HttpServletRequest) req).getHeader("authorization");
        String userNpasswd = "";
        if (authorizationHeader != null) {
            userNpasswd = authorizationHeader.split("Basic ")[1];
        }

        if (PropertiesHelper.doRequestLoging()) {
            RequestWapper my = new RequestWapper();
            my.setMethod(method);
            my.setContentLength(contentLen);
            my.setContentType(contentType);
            my.setTimeStamp(System.currentTimeMillis());
            my.setElapsed(elapsed);
            my.setRemoteAddr(from);
            my.setRequestURL(((HttpServletRequest) req).getRequestURL().toString());
            my.setUserAgent(userAgent);
            my.setUserNpasswd(getUserName((HttpServletRequest) req));
            queue.add(my);
            startRecorder();
        }

        log.log(Level.INFO, "Req_Source: {0} Method: {1} Content_Len: {2} Content_Type: {3} Elapsed_Time: {4} sec EncodedUser: {5} UserAgent: {6}", new Object[]{from, method, contentLen, contentType, elapsed / 1000.0, userNpasswd, userAgent});
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

//    private void recordEvent(Connection connection, HttpServletRequest httpServletRequest, double elapsed) throws SQLException, UnsupportedEncodingException {
//        getCatalogue().recordRequest(connection, httpServletRequest, elapsed);
//        connection.commit();
//    }
    private Predictor getPredictor() throws Exception {
        if (PropertiesHelper.doPrediction()) {
            //TODO: Use class loader 
            if (predictor == null) {
                String algorithm = PropertiesHelper.getPredictorAlgorithm();
                if (algorithm.equals("FirstSuccessor")) {
                    predictor = new FirstSuccessor();
                }
                if (algorithm.equals("LastSuccessor")) {
                    predictor = new LastSuccessor();
                }
                if (algorithm.equals("StableSuccessor")) {
                    predictor = new StableSuccessor();
                }
                if (algorithm.equals("RecentPopularity")) {
                    predictor = new RecentPopularity();
                }
                if (algorithm.equals("PredecessorPosition")) {
                    predictor = new PredecessorPosition();
                }

                if (algorithm.equals("MarkovPredictor")) {
                    predictor = new MarkovPredictor();
                }
                if (algorithm.equals("ClusterPredictor")) {
                    predictor = new ClusterPredictor();
                }
            }
        }

        return predictor;
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
//            if (getPredictor() != null) {
//                getPredictor().stop();
//            }

            if (recordertimer != null) {
                if (recorder != null) {
                    recorder.comitToDB();
                }
                this.recordertimer.cancel();
            }

        } catch (Exception ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void predict(Method method, String reqURL) throws Exception {
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

        nextState = getPredictor().getNextState(currentState);

        if (prevState != null) {
            getPredictor().setPreviousStateForCurrent(prevState, currentState);
        }
        if (prevPrediction != null) {
            if (currentState.getID().equals(prevPrediction.getID())) {
                log.log(Level.INFO, "Prediction Result: Hit");
            } else {
                log.log(Level.INFO, "Prediction Result: Miss");
            }
        } else {
            log.log(Level.INFO, "Prediction Result: Non");
        }

        prevState = currentState;
        prevPrediction = nextState;
    }

    private void startRecorder() {
        if (recorder == null) {
            recorder = new RequestEventRecorder(queue, getCatalogue());
            TimerTask gcTask = new MyTask(recorder);

            recordertimer = new Timer(true);
            recordertimer.schedule(gcTask, 400, 400);
        }
    }

    private static class RequestEventRecorder implements Runnable {

        private final BlockingQueue queue;
        private final JDBCatalogue catalogue;
        private List<RequestWapper> req = new ArrayList<>();

        private RequestEventRecorder(BlockingQueue queue, JDBCatalogue catalogue) {
            this.queue = queue;
            this.catalogue = catalogue;
        }

        @Override
        public void run() {
            try {
                RequestWapper rw = (RequestWapper) queue.take();
//                log.log(Level.FINE, "RequestWapper: {0} {1} {2} {3} {4} {5} {6} {7} {8}", new Object[]{rw.contentType, rw.method, rw.remoteAddr, rw.requestURL, rw.userAgent, rw.userNpasswd, rw.contentLength, rw.date, rw.elapsed});
                req.add(rw);
                if (req.size() >= 50) {
                    try {
                        comitToDB();
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void comitToDB() throws UnsupportedEncodingException {
            if (req != null && !req.isEmpty()) {
                try (Connection connection = catalogue.getConnection()) {
                    catalogue.recordRequests(connection, req);
                    connection.commit();
                    req.clear();
//                connection.close();
                } catch (SQLException ex) {
                    Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}

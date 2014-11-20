/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class AuthWorker implements AuthI {

    private static String theToken;

//    private static final Map<String, Integer> temporarryTokens = new HashMap<>();
//    private static final List<String> temporarryTokens = new ArrayList<>();
    static {
        try {
//            temporarryTokens.add(PropertiesHelper.getWorkerToken());
            theToken = PropertiesHelper.getWorkerToken();
        } catch (IOException ex) {
            Logger.getLogger(AuthWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public MyPrincipal checkToken(String token) {
        try {
            MyPrincipal principal = null;
            if (theToken == null) {
                theToken = PropertiesHelper.getWorkerToken();
            }

            if (theToken.equals(token)) {
                HashSet<String> roles = new HashSet<>();
                roles.add("admin");
                principal = new MyPrincipal("worker-", roles, token);
            }
            return principal;
        } catch (Exception ex) {
            Logger.getLogger(AuthWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void setTicket(String workerID, String token) {
//        synchronized (temporarryTokens) {
////            temporarryTokens.put(token, 0);
//            if (!temporarryTokens.contains(token)) {
//                temporarryTokens.add(token);
//            }
//        }
    }
}

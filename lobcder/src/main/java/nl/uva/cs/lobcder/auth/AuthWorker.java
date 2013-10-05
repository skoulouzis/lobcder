/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class AuthWorker implements AuthI {

//    private static final Map<String, Integer> temporarryTokens = new HashMap<>();
    private static final List<String> temporarryTokens = new ArrayList<>();

    @Override
    public MyPrincipal checkToken(String token) {
        MyPrincipal principal = null;
        synchronized (temporarryTokens) {
//            if (temporarryTokens.containsKey(token)) {
//                Integer timesAuth = temporarryTokens.get(token);
//                temporarryTokens.put(token, timesAuth++);
//                Logger.getLogger(AuthWorker.class.getName()).log(Level.FINE, "Token timesAuth: " + timesAuth);
//                HashSet<String> roles = new HashSet<>();
//                roles.add("admin");
//                principal = new MyPrincipal("worker-", roles);
////                if (timesAuth >= 3) {
////                    temporarryTokens.remove(token);
////                }
//
//            }

            for (String t : temporarryTokens) {
                if (t.equals(token)) {
                    HashSet<String> roles = new HashSet<>();
                    roles.add("admin");
                    principal = new MyPrincipal("worker-", roles);
//                    Logger.getLogger(AuthWorker.class.getName()).log(Level.FINE, "Check token: {0}", token);
                    temporarryTokens.remove(token);
//                    Logger.getLogger(AuthWorker.class.getName()).log(Level.FINE, "temporarryTokens.size(): {0}", temporarryTokens.size());
//                    if (temporarryTokens.size() >= 2048) {
//                        temporarryTokens.remove(0);
//                    }
                    break;
                }
            }

//            String workerID = temporarryTokens.get(token);
//            log.log(Level.FINE, "Cheking: {0}", token);
//            if (workerID != null) {
//                HashSet<String> roles = new HashSet<>();
//                roles.add("admin");
//                principal = new MyPrincipal(workerID, roles);
//                temporarryTokens.remove(token);
//            }


//        HashSet<String> roles = new HashSet<>();
//        roles.add("admin");
//        principal = new MyPrincipal("worker-", roles);

//        Logger.getLogger(AuthWorker.class.getName()).log(Level.FINE, "Returning principal: {0}", principal);
        return principal;
        }
    }

    public static void setTicket(String workerID, String token) {
        synchronized (temporarryTokens) {
//            temporarryTokens.put(token, 0);
            if (!temporarryTokens.contains(token)) {
                temporarryTokens.add(token);
            }
        }
    }
}

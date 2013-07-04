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
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class AuthWorker implements AuthI {

    private static final List<String> temporarryTokens = new ArrayList<>(10);

    @Override
    public MyPrincipal checkToken(String token) {
        MyPrincipal principal = null;
        synchronized (temporarryTokens) {
            for (String t : temporarryTokens) {
                if (t.equals(token)) {
                    HashSet<String> roles = new HashSet<>();
                    roles.add("admin");
                    principal = new MyPrincipal("worker-", roles);
                    break;
                }
            }
//            temporarryTokens.remove(token);
//            String workerID = temporarryTokens.get(token);
//            log.log(Level.FINE, "Cheking: {0}", token);
//            if (workerID != null) {
//                HashSet<String> roles = new HashSet<>();
//                roles.add("admin");
//                principal = new MyPrincipal(workerID, roles);
//                temporarryTokens.remove(token);
//            }
            return principal;
        }
    }

    public static void setTicket(String workerID, String token) {
        synchronized (temporarryTokens) {
//            temporarryTokens.put(token, workerID);
            if (!temporarryTokens.contains(token)) {
                temporarryTokens.add(token);
            }
            if (temporarryTokens.size() >= 40) {
                temporarryTokens.remove(0);
            }
        }
    }
}

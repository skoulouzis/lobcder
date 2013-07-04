/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import lombok.extern.java.Log;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class AuthWorker implements AuthI {

    private static Map<String, String> temporarryTokens = new HashMap<>();

    @Override
    public MyPrincipal checkToken(String token) {
        String workerID = temporarryTokens.get(token);
        MyPrincipal principal = null;
        log.log(Level.FINE, "Cheking: " + token);
        if (workerID != null) {
            HashSet<String> roles = new HashSet<>();
            roles.add("admin");
            principal = new MyPrincipal(workerID, roles);
            temporarryTokens.remove(token);
        }
        return principal;
    }

    public static void setTicket(String workerID, String token) {
        temporarryTokens.put(token, workerID);
    }
}

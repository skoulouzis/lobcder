/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dvasunin
 */
public class PrincipalCache implements PrincipalCacheI {

    private static class TimedPrincipal {

        public final long date;
        public final MyPrincipal principal;

        TimedPrincipal(long date, MyPrincipal principal) {
            this.date = date;
            this.principal = principal;
        }
    }
    private Map<String, TimedPrincipal> cache = new HashMap<>();

    @Override
    public synchronized MyPrincipal getPrincipal(String token) {
        TimedPrincipal res = cache.get(token);
        if (res != null) {
            if (res.date >= new Date().getTime()) {
                return res.principal;
            } else {
                cache.remove(token);
            }
        }
        return null;
    }

    @Override
    public synchronized void putPrincipal(String token, MyPrincipal principal, long exp_date) {
        cache.put(token, new TimedPrincipal(exp_date, principal));
    }
}

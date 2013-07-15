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

        public final Date date;
        public final MyPrincipal principal;

        TimedPrincipal(Date date, MyPrincipal principal) {
            this.date = date;
            this.principal = principal;
        }
    }
    private Map<String, TimedPrincipal> cache = new HashMap<String, TimedPrincipal>();
    private long timeout; // msec
    public final static PrincipalCache pcache = new PrincipalCache();

    @Override
    public synchronized MyPrincipal getPrincipal(String token) {
        TimedPrincipal res = cache.get(token);
        if (res != null) {
            if (new Date(res.date.getTime() + getTimeout()).after(new Date())) {
                return res.principal;
            } else {
                cache.remove(token);
            }
        }
        return null;
    }

    @Override
    public synchronized void putPrincipal(String token, MyPrincipal principal) {
        cache.put(token, new TimedPrincipal(new Date(), principal));
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

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
public class PrincipalCache {
    
    
    private Map<String, MyPrincipal> cache = new HashMap<String, MyPrincipal>();
    private long timeout; // msec
    
    public final static PrincipalCache pcache = new PrincipalCache();
    
    public synchronized  MyPrincipal getPrincipal(String token){
        MyPrincipal res = cache.get(token);
        if(res != null){
            if(new Date(res.getDate().getTime() + getTimeout()).after(new Date()))
                return res;
            else
                cache.remove(token);
        }
        return null;
    }
    
    public synchronized void putPrincipal(MyPrincipal principal){
        cache.put(principal.getToken(), principal);
        principal.setDate(new Date());
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.Utils;
import java.util.Date;

/**
 *
 * @author alogo
 */
class CurrentLock {

    protected String lockId;
    protected final Long seconds;
    protected final LockInfo lockInfo;

    public CurrentLock(Date date, String toString, Long seconds, LockInfo lockInfo) {
        this.seconds = seconds;
        this.lockInfo = lockInfo;
    }

    CurrentLock refresh() {

        Date dt = Utils.addSeconds(Utils.now(), seconds);

        return new CurrentLock(dt, lockId, seconds, lockInfo);

    }
}

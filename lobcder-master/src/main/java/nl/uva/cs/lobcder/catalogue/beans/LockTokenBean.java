package nl.uva.cs.lobcder.catalogue.beans;

import io.milton.http.LockInfo;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.TokensDeleteSweep;

/**
 * User: dvasunin Date: 03.03.14 Time: 16:12 To change this template use File |
 * Settings | File Templates.
 */
@XmlRootElement(name = "lock")
@XmlAccessorType(XmlAccessType.FIELD)
public class LockTokenBean {

    private String locktoken;
    private LockInfo lockInfo;
    private XMLGregorianCalendar lockedUntil;

    public LockToken getLock() {
        long timeout = getLockedUntil().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();
        return new LockToken(getLocktoken(), getLockInfo(), new LockTimeout(timeout));
    }

    public void setLock(LockToken lockToken) {
        try {
            this.setLocktoken(lockToken.tokenId);
            this.setLockInfo(lockToken.info);
            GregorianCalendar gCalendar = new GregorianCalendar();
            gCalendar.setTimeInMillis(lockToken.timeout.getSeconds() * 1000 + System.currentTimeMillis());
            setLockedUntil(DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar));
        } catch (Exception e) {
            Logger.getLogger(LockTokenBean.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * @return the locktoken
     */
    public String getLocktoken() {
        return locktoken;
    }

    /**
     * @param locktoken the locktoken to set
     */
    public void setLocktoken(String locktoken) {
        this.locktoken = locktoken;
    }

    /**
     * @return the lockInfo
     */
    public LockInfo getLockInfo() {
        return lockInfo;
    }

    /**
     * @param lockInfo the lockInfo to set
     */
    public void setLockInfo(LockInfo lockInfo) {
        this.lockInfo = lockInfo;
    }

    /**
     * @return the lockedUntil
     */
    public XMLGregorianCalendar getLockedUntil() {
        return lockedUntil;
    }

    /**
     * @param lockedUntil the lockedUntil to set
     */
    public void setLockedUntil(XMLGregorianCalendar lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

}

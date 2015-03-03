package nl.uva.cs.lobcder.catalogue.beans;

import io.milton.http.LockInfo;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;

/**
 * User: dvasunin
 * Date: 03.03.14
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
@Log
@NoArgsConstructor
@XmlRootElement(name = "lock")
@XmlAccessorType(XmlAccessType.FIELD)
public class LockTokenBean {
    private String locktoken;
    private LockInfo lockInfo;
    private XMLGregorianCalendar lockedUntil;

    public LockToken getLock(){
        long timeout = lockedUntil.toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();
        return new LockToken(locktoken, lockInfo, new LockTimeout(timeout));
    }

    public void setLock(LockToken lockToken) {
        try{
            this.locktoken = lockToken.tokenId;
            this.lockInfo = lockToken.info;
            GregorianCalendar gCalendar = new GregorianCalendar();
            gCalendar.setTimeInMillis(lockToken.timeout.getSeconds() * 1000 + System.currentTimeMillis()) ;
            lockedUntil = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
        } catch (Exception e) {
            log.log(Level.SEVERE, null, e);
        }
    }

}

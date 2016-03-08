package nl.uva.cs.lobcder.catalogue.beans;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * User: dvasunin
 * Date: 27.02.14
 * Time: 17:00
 * To change this template use File | Settings | File Templates.
 */


@XmlRootElement(name = "dri")
@XmlAccessorType(XmlAccessType.FIELD)
public class DriBean {

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the lastValidationDate
     */
    public XMLGregorianCalendar getLastValidationDate() {
        return lastValidationDate;
    }

    /**
     * @param lastValidationDate the lastValidationDate to set
     */
    public void setLastValidationDate(XMLGregorianCalendar lastValidationDate) {
        this.lastValidationDate = lastValidationDate;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }
    public static enum Status {unavailable, corrupted, OK};
    private String checksum;
    private XMLGregorianCalendar lastValidationDate;
    private Status status;
}

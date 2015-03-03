package nl.uva.cs.lobcder.catalogue.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * User: dvasunin
 * Date: 27.02.14
 * Time: 17:00
 * To change this template use File | Settings | File Templates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "dri")
@XmlAccessorType(XmlAccessType.FIELD)
public class DriBean {
    public static enum Status {unavailable, corrupted, OK};
    private String checksum;
    private XMLGregorianCalendar lastValidationDate;
    private Status status;
}

package nl.uva.cs.lobcder.rest.wrappers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Link {

    @XmlElement(name = "src-switch")
    public String srcSwitch;
    @XmlElement(name = "src-port")
    public int srcPort;
    @XmlElement(name = "dst-switch")
    public String dstSwitch;
    @XmlElement(name = "dst-port")
    public int dstPort;
    @XmlElement(name = "type")
    public String type;
    @XmlElement(name = "direction")
    public String direction;
}
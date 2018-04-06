package nl.uva.cs.lobcder.rest.wrappers;

import java.util.List;

public class NetworkEntity {

    private List<AttachmentPoint> attachmentPoint;
    private String entityClass;
    private List<String> ipv4;
    private Number lastSeen;
    private List<String> mac;
    private List<String> vlan;

    public List<AttachmentPoint> getAttachmentPoint() {
        return this.attachmentPoint;
    }

    public void setAttachmentPoint(List<AttachmentPoint> attachmentPoint) {
        this.attachmentPoint = attachmentPoint;
    }

    public String getEntityClass() {
        return this.entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public List<String> getIpv4() {
        return this.ipv4;
    }

    public void setIpv4(List<String> ipv4) {
        this.ipv4 = ipv4;
    }

    public Number getLastSeen() {
        return this.lastSeen;
    }

    public void setLastSeen(Number lastSeen) {
        this.lastSeen = lastSeen;
    }

    public List<String> getMac() {
        return this.mac;
    }

    public void setMac(List<String> mac) {
        this.mac = mac;
    }

    public List<String> getVlan() {
        return this.vlan;
    }

    public void setVlan(List<String> vlan) {
        this.vlan = vlan;
    }
}

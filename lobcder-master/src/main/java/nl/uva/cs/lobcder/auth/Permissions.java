/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;


import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The permissions for a resource. 
 * @author dvasunin
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Permissions {

    @XmlTransient
    private Long localId;
    private Set<String> read = new HashSet<>();
    private Set<String> write = new HashSet<>();
    private String owner = "";

    public Permissions() {
    }

//    public Permissions(MyPrincipal mp) {
//        owner = mp.getUserId();
//        read.addAll(mp.getRoles());
//    }

    public Permissions(MyPrincipal mp, Permissions parentPermissions) {
        owner = mp.getUserId();
        read.addAll(parentPermissions.getRead());
        // Do not include the roles the user belongs to to the list of readers. For security reason.
        //read.addAll(mp.getRoles());
        write.addAll(parentPermissions.getWrite());
    }

    public String getReadStr() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : getRead()) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);
            }
        }
        return sb.toString();
    }

    public String getWriteStr() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : getWrite()) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);
            }
        }
        return sb.toString();
    }

    /**
     * @return the localId
     */
    public Long getLocalId() {
        return localId;
    }

    /**
     * @param localId the localId to set
     */
    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    /**
     * @return the read
     */
    public Set<String> getRead() {
        return read;
    }

    /**
     * @param read the read to set
     */
    public void setRead(Set<String> read) {
        this.read = read;
    }

    /**
     * @return the write
     */
    public Set<String> getWrite() {
        return write;
    }

    /**
     * @param write the write to set
     */
    public void setWrite(Set<String> write) {
        this.write = write;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }
}

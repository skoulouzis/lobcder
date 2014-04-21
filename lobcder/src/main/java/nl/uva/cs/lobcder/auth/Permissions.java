/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author dvasunin
 */
@Data
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
        read.retainAll(mp.getRoles());
    }

    public String getReadStr() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : read) {
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
        for (String s : write) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(',').append(s);
            }
        }
        return sb.toString();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author dvasunin
 */
@Data
@XmlRootElement
public class Permissions {

    private Set<String> read = new HashSet<String>();
    private Set<String> write = new HashSet<String>();
    private String owner = "";

    public Permissions() {
    }

    public Permissions(MyPrincipal mp) {
        owner = mp.getUserId();
        read.addAll(mp.getRoles());
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

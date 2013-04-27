package nl.uva.cs.lobcder.resources;

import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * User: dvasunin Date: 01.03.13 Time: 13:57 To change this template use File |
 * Settings | File Templates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
public class PDRIDescr {

    private String name;
    @Getter(AccessLevel.NONE)
    private Long storageSiteId;
    private String resourceUrl;
    private String username;
    private String password;
    private boolean encrypt;

    @XmlTransient
    public Long getStorageSiteId() {
        return storageSiteId;
    }

    boolean getEncrypt() {
        return encrypt;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name="site")
@XmlAccessorType(XmlAccessType.FIELD)
public class StorageSiteBean {
    private Long id;
    private String uri;
    private Boolean encrypt;
    private Boolean cache;
    private String extra;
    private CredentialBean credential;

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof StorageSiteBean)) return false;
        StorageSiteBean other = (StorageSiteBean) o;
        if (other.getId().equals(getId())) return true;
        else return false;
     }
}

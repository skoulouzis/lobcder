/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.uva.cs.lobcder.rest.wrappers.CredentialWrapped;

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
}

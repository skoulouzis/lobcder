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
@XmlRootElement(name="credential")
@XmlAccessorType(XmlAccessType.FIELD)
public class CredentialBean {
    private String username;
    private String password;
}

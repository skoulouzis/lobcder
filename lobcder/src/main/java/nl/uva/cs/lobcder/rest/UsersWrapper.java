/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
@Data
public class UsersWrapper {
    private Long id;
    private String uname;
    private String token;
    private List<String> roles;
}

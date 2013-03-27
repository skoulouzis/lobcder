/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import lombok.Data;
import nl.uva.vlet.util.cog.GridProxy;
import java.io.Serializable;

/**
 *
 * @author S. Koulouzis
 */
@Data
public class Credential implements Serializable{
    private String storageSiteUsername;
    private String storageSitePassword;
    private GridProxy gridProxy;
}

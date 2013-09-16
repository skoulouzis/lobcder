/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.Serializable;
import lombok.Data;
import nl.uva.vlet.util.cog.GridProxy;

/**
 *
 * @author S. Koulouzis
 */
@Data
public class Credential implements Serializable {

    private String storageSiteUsername;
    private String storageSitePassword;
    private GridProxy gridProxy;
}

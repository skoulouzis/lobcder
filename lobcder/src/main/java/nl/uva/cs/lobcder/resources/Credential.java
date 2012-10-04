/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import nl.uva.cs.lobcder.resources.*;
import java.io.Serializable;
import nl.uva.vlet.util.cog.GridProxy;

/**
 *
 * @author S. Koulouzis
 */
public class Credential implements Serializable{

    private String storageSiteUname;
    private String passwd;
    private GridProxy gridProxy;

    public Credential() {
    }

    public GridProxy getStorageSiteGridProxy() {
        return this.gridProxy;
    }

    public String getStorageSiteUsername() {
        return storageSiteUname;
    }

    public String getStorageSitePassword() {
        return passwd;
    }

    public void setStorageSiteUsername(String storageSiteUname) {
        this.storageSiteUname = storageSiteUname;
    }

    public void setStorageSitePassword(String passwd) {
        this.passwd = passwd;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.Serializable;
import nl.uva.vlet.util.cog.GridProxy;

/**
 *
 * @author S. Koulouzis
 */
public class Credential implements Serializable {

    private Long id;
    private String storageSiteUsername;
    private String storageSitePassword;
    private GridProxy gridProxy;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the storageSiteUsername
     */
    public String getStorageSiteUsername() {
        return storageSiteUsername;
    }

    /**
     * @param storageSiteUsername the storageSiteUsername to set
     */
    public void setStorageSiteUsername(String storageSiteUsername) {
        this.storageSiteUsername = storageSiteUsername;
    }

    /**
     * @return the storageSitePassword
     */
    public String getStorageSitePassword() {
        return storageSitePassword;
    }

    /**
     * @param storageSitePassword the storageSitePassword to set
     */
    public void setStorageSitePassword(String storageSitePassword) {
        this.storageSitePassword = storageSitePassword;
    }

    /**
     * @return the gridProxy
     */
    public GridProxy getGridProxy() {
        return gridProxy;
    }

    /**
     * @param gridProxy the gridProxy to set
     */
    public void setGridProxy(GridProxy gridProxy) {
        this.gridProxy = gridProxy;
    }
}

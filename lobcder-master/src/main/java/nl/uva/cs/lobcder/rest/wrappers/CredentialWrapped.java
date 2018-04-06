/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
public class CredentialWrapped {

    private String storageSiteUsername;
    private String storageSitePassword;

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
}

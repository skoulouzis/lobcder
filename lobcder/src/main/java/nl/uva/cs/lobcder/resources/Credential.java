/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.uva.cs.lobcder.resources;

import nl.uva.vlet.util.cog.GridProxy;

/**
 *
 * @author S. Koulouzis
 */
public class Credential {
    private String vphUsername;
    private String storageSiteUname;
    private String passwd;

    public GridProxy getStorageSiteGridProxy() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    String getStorageSiteUsername() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    String getStorageSitePassword() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    String getVPHUsername() {
        return vphUsername;
    }

    void setStorageSiteUsername(String storageSiteUname) {
        this.storageSiteUname = storageSiteUname;
    }

    void setStorageSitePassword(String passwd) {
        this.passwd = passwd;
    }

    void setVPHUsernname(String vphUsername) {
        this.vphUsername = vphUsername;
    }

}

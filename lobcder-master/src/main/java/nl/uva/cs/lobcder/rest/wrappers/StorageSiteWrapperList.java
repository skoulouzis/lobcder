/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
public class StorageSiteWrapperList {
    private List<StorageSiteWrapper> sites;

    /**
     * @return the sites
     */
    public List<StorageSiteWrapper> getSites() {
        return sites;
    }

    /**
     * @param sites the sites to set
     */
    public void setSites(List<StorageSiteWrapper> sites) {
        this.sites = sites;
    }
}

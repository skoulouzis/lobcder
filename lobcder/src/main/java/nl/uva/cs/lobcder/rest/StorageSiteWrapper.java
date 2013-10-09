/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
@Data
class StorageSiteWrapper {

    private Long storageSiteId;
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private CredentialWrapped credential;
    private boolean encrypt;
    private boolean isCache;

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import java.io.Serializable;
import java.net.URI;
import java.util.Properties;
import javax.jdo.annotations.PersistenceCapable;

/**
 *
 * @author S. Koulouzis
 */

@PersistenceCapable
public class StorageResource implements Serializable, IStorageResource {

    /**
     * 
     */
    private static final long serialVersionUID = -2552461454620784560L;
    private URI storageLocation;
    private Credential cred;
    private Permissions perm;
    private Properties prop;

    public StorageResource(URI storageLocation, Credential cred) {
        this.storageLocation = storageLocation;
        this.cred = cred;
    }

    @Override
    public URI getStorageLocation() {
        return storageLocation;
    }
}

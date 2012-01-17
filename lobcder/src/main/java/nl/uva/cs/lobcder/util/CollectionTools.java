/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.util.ArrayList;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.StorageSite;

/**
 *
 * @author S. koulouzis
 */
public class CollectionTools {

    public static Collection<StorageSite> combineStorageSites(Collection<StorageSite> storageSitesA, Collection<StorageSite> storageSitesB) {
         Collection<StorageSite> notInSA = new ArrayList<StorageSite>();
        for(StorageSite sB: storageSitesB){
            for(StorageSite sA : storageSitesA){
                if(!sB.getEndpoint().equals(sA.getEndpoint()) &&
                        !sB.getVPHUsername().equals(sA.getVPHUsername())&&
                        !sB.getCredentials().getStorageSiteUsername().equals(sA.getCredentials().getStorageSiteUsername())){
                    notInSA.add(sB);
                }
            }
        }
        storageSitesA.addAll(notInSA);
        return storageSitesA;
    }
    
}

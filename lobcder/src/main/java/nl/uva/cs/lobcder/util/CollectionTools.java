/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.util.ArrayList;
import java.util.Collection;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.StorageSite;

/**
 *
 * @author S. koulouzis
 */
public class CollectionTools {

    public static Collection<IStorageSite> combineStorageSites(Collection<IStorageSite> storageSitesA, Collection<IStorageSite> storageSitesB) {
         ArrayList<IStorageSite> notInSA = new ArrayList<IStorageSite>();
        for(IStorageSite sB: storageSitesB){
            for(IStorageSite sA : storageSitesA){
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

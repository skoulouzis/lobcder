package com.ettrema.http;

import com.bradmcevoy.common.InternationalizedString;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.values.Pair;
import java.util.List;

/**
 * Interface for collections which can be used as address books for CARDDAV
 *
 * Must implement CalendarCollection as there is a cross-over of property support
 * 
 * @author bradm
 */
public interface AddressBookResource  extends CalendarCollection, PropFindableResource {
    /**
     * This property contains a description of the address book collection that 
     * is suitable for presentation to a user. The xml:lang attribute can be used 
     * to add a language tag for the value of this property.
     * 
     * @return Provides a human-readable description of the address book collection.
     */
    InternationalizedString getDescription();
    
    /**
     * This property contains a description of the address book collection that 
     * is suitable for presentation to a user. The xml:lang attribute can be used 
     * to add a language tag for the value of this property.
     * 
     * @param description is a human-readable description of the address book collection.
     */
    void setDescription(InternationalizedString description);
    
    /**
     * property is used to specify the media type supported for the address 
     * object resources contained in a given address book collection 
     * (e.g., vCard version 3.0). Any attempt by the client to store address object resources
     * with a media type not listed in this property MUST result in an error, 
     * with the CARDDAV:supported-address-data precondition (Section 6.3.2.1) 
     * being violated. In the absence of this property, the server MUST only 
     * accept data with the media type "text/vcard" and vCard version 3.0, 
     * and clients can assume that is all the server will accept.
     * 
     * @return 
     */
    List<Pair<String, String>> getSupportedAddressData();
    
    /**
     * This property is used to specify a numeric value that represents the 
     * maximum size in octets that the server is willing to accept when an 
     * address object resource is stored in an address book collection. 
     * Any attempt to store an address book object resource exceeding this size 
     * MUST result in an error, with the CARDDAV:max-resource-size precondition 
     * (Section 6.3.2.1) being violated. In the absence of this 
     * property, the client can assume that the server will allow storing 
     * a resource of any reasonable size.
     * 
     * @return 
     */
    Long getMaxResourceSize();
    
}

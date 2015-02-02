package com.ettrema.http;

import com.bradmcevoy.http.Resource;
/**
 * Represents an address resource. 
 * 
 * Example(1):
 * <C:address-data>.......................</C:address-data>
 * 
 * 
 * Example(2): (Not Supported yet)
 * <C:address-data>
 *   <C:prop name="VERSION"/>
 *   <C:prop name="UID"/>
 *   <C:prop name="NICKNAME"/>
 *   <C:prop name="EMAIL"/>
 *   <C:prop name="FN"/>
 * </C:address-data>
 * 
 * @author nabil.shams
 */
public interface AddressResource extends Resource{
    String getAddressData();
}

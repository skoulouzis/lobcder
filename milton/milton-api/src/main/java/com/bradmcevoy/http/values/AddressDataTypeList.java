package com.bradmcevoy.http.values;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Holds a list of Pair<String, String>, i.e. actually address data Type, where 
 * object1 represents ContentType and object2 represents the version.
 * 
 * @author nabil.shams
 */
public class AddressDataTypeList extends ArrayList<Pair<String, String>>{
    public static AddressDataTypeList asList(Pair<String, String>... items) {
        AddressDataTypeList list = new AddressDataTypeList();
		list.addAll(Arrays.asList(items));
        return list;
    }
}

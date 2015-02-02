package com.bradmcevoy.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author brad
 */
public abstract class StringUtils {

    public static String[] delimitedListToStringArray( String str, String delimiter ) {
        if( str == null ) {
            return new String[0];
        }
        if( delimiter == null ) {
            return new String[]{str};
        }
        List result = new ArrayList();
        if( "".equals( delimiter ) ) {
            for( int i = 0; i < str.length(); i++ ) {
                result.add( str.substring( i, i + 1 ) );
            }
        } else {
            int pos = 0;
            int delPos = 0;
            while( ( delPos = str.indexOf( delimiter, pos ) ) != -1 ) {
                result.add( str.substring( pos, delPos ) );
                pos = delPos + delimiter.length();
            }
            if( str.length() > 0 && pos <= str.length() ) {
                // Add rest of String, but not in case of empty input.
                result.add( str.substring( pos ) );
            }
        }
        return toStringArray( result );
    }

    public static String[] toStringArray( Collection collection ) {
        if( collection == null ) {
            return null;
        }
        return (String[]) collection.toArray( new String[collection.size()] );
    }

	/**
	 * 
	 * true iff the given string contains any of the given strings
	 * 
	 * @param ua
	 * @param browserIds
	 * @return 
	 */
	public static boolean contains(String ua, String[] browserIds) {	
		for(String s : browserIds ) {
			if( ua.contains(s)) {
				return true;
			}
		}
		return false;
	}
}

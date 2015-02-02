package com.bradmcevoy.http.webdav;

import com.bradmcevoy.property.PropertySource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class PropertySourcesList extends ArrayList<PropertySource> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an empty list
     */
    public PropertySourcesList() {
    }

    /**
     * Adds all default property sources
     * 
     * @param resourceTypeHelper
     */
    public PropertySourcesList( ResourceTypeHelper resourceTypeHelper ) {
        this.addAll( PropertySourceUtil.createDefaultSources( resourceTypeHelper ) );
    }

    /**
     * Allows you to add an extra source to the default list
     *
     * @param source
     */
    public void setExtraSource( PropertySource source ) {
        this.add( source );
    }

    public void setSources( List<PropertySource> sources ) {
        this.clear();
        this.addAll( sources );
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for( PropertySource l : this ) {
			sb.append(l.getClass()).append(",");
		}
		return sb.toString();
	}
	
	
}

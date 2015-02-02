package com.bradmcevoy.http.webdav;

import com.bradmcevoy.property.BeanPropertySource;
import com.bradmcevoy.property.CustomPropertySource;
import com.bradmcevoy.property.MultiNamespaceCustomPropertySource;
import com.bradmcevoy.property.PropertySource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author brad
 */
public class PropertySourceUtil {
    /**
     * Create default extension property sources. These are those additional
     * to the webdav default properties defined on the protocol itself
     *
     * @param resourceTypeHelper
     * @return
     */
    public static List<PropertySource> createDefaultSources(ResourceTypeHelper resourceTypeHelper) {
        List<PropertySource> list = new ArrayList<PropertySource>();
        CustomPropertySource customPropertySource = new CustomPropertySource();
        list.add( customPropertySource );
        MultiNamespaceCustomPropertySource mncps = new MultiNamespaceCustomPropertySource();
        list.add( mncps );
        BeanPropertySource beanPropertySource = new BeanPropertySource();
        list.add( beanPropertySource);
        return list;
    }
}

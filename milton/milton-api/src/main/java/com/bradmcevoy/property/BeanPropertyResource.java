package com.bradmcevoy.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation type to identify classes to be accessible by
 * BeanPropertySource
 *
 * This allows them to have their properties read from and written to
 * by PROPFIND and PROPPATCH.
 *
 * @author brad
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanPropertyResource {

    /**
     * Default property which is the namespace uri for the properties
     * on this resource
     *
     * E.g. http://mycompany.com/ns/example
     * 
     * @return - the namespace uri
     */
    String value();
    /**
     *
     * @return - true allows the resource to be updatable
     */
    boolean writable() default true;

    /**
     * If true, indicates that properties on the resource should be accessible
     * unless otherwise specified
     * 
     * @return
     */
    boolean enableByDefault() default true;
}

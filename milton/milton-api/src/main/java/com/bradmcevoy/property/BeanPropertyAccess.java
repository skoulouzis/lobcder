package com.bradmcevoy.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation type to identify properties on classes annotation with
 * BeanPropertyResource which should or should not be accessed through
 * BeanPropertySource
 *
 * @author brad
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeanPropertyAccess {

    /**
     * True indicates that the method should be enabled (ie DAV accessible)
     * regardless of the class default
     *
     * False indicats that the property is accessible regardless of class default
     *
     * @return
     */
    boolean value();
}

package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.Resource;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * Assists in determining the logical type of a given resource, for the
 * purpose of the protocol
 *
 * @author brad
 */
public interface ResourceTypeHelper {
    /**
     * Get the resource types for a PROPFIND request. E.g. collection, calendar, etc
     *
     * @param r
     * @return - a list of QName's where the URI determines the namespace (E.g. DAV,
     * http://calendarserver.org/ns/) and the name is the name of the resource type
     * E.g. collection, calendar
     */
    List<QName> getResourceTypes(Resource r);


    /**
     * Gets the list of supported level names for a resource. This is to populate
     * the DAV header, E.g. 1, access-control, calendar-access
     *
     * Typically, this list is determined by the type of the resource. Eg, if the
     * resource supports locking then it returns 1, 2. Note that should **NOTE** be
     * sensitive to authorisation or state. Ie a resource should have supported levels
     * of 1,2 if it supports locking, regardless of whether or not the current user
     * has permission to do so, and regardless of whether the resource can be locked in
     * its current state.
     *
     * @param r - the resource
     * @return - the list of supported level identifiers supported by the given resource
     */
    List<String> getSupportedLevels(Resource r);
}

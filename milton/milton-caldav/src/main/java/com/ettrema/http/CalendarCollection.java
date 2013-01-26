package com.ettrema.http;

import com.bradmcevoy.http.CollectionResource;

/**
 * Base interface for scheduling inboxes and outboxes
 *
 * @author brad
 */
public interface CalendarCollection extends CollectionResource {
    /**
     *  For each calendar or scheduling Inbox or Outbox collection on the
     *   server, a new CS:getctag WebDAV property is present.
     *
     *   The property value is an "opaque" token whose value is guaranteed to
     *   be unique over the lifetime of any calendar or scheduling Inbox or
     *   Outbox collection at a specific URI.
     *
     *   Whenever a calendar resource is added to, modified or deleted from
     *   the calendar collection, the value of the CS:getctag property MUST
     *   change.  Typically this change will occur when the DAV:getetag
     *   property on a child resource changes due to some protocol action.  It
     *   could be the result of a change to the body or properties of the
     *   resource.

     *
     * http://svn.calendarserver.org/repository/calendarserver/CalendarServer/trunk/doc/Extensions/caldav-ctag.txt
     *
     * @return
     */
    String getCTag();
    
}

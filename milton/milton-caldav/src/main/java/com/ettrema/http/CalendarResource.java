package com.ettrema.http;

import com.bradmcevoy.http.PropFindableResource;

/**
 *
 * @author alex
 */
public interface CalendarResource extends CalendarCollection, PropFindableResource {

    String getCalendarDescription();

    String getColor();
    
    void setColor(String s);	

}

package com.ettrema.event;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 *
 * @author brad
 */
public interface EventManager {
    void fireEvent(Event e) throws ConflictException, BadRequestException, NotAuthorizedException;
    <T extends Event> void registerEventListener(EventListener l, Class<T> c);
}

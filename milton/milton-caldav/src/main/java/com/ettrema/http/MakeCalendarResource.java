package com.ettrema.http;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 *
 * @author brad
 */
public interface MakeCalendarResource extends CollectionResource {

    /**
     * Create an empty calendar
     *
     * @param newName
     * @return
     * @throws NotAuthorizedException
     * @throws ConflictException
     * @throws BadRequestException
     */
    CollectionResource createCalendar(String newName) throws NotAuthorizedException, ConflictException, BadRequestException;

}

package com.ettrema.http.report;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 * Represents a known report type, is delegated to by the ReportHandler
 *
 * @author brad
 */
public interface Report {
    /**
     * The name of the report, as used in REPORT requests
     * 
     * @return
     */
    String getName();

    /**
     * Process the requested report body, and return a document containing the
     * response body.
     *
     * Must be a multistatus response.
     *
     * @param host 
     * @param r 
     * @param doc
     * @return the response body, usually xml
     */
    String process(String host, String path, Resource r, org.jdom.Document doc) throws BadRequestException, ConflictException, NotAuthorizedException;
}

package com.bradmcevoy.http;

/**
 * Just a marker interface to indicate that the resource is permitted to support
 * REPORT requests.
 *
 * The requirements for supporting particular Report instances is determined
 * by each report, but is typically just to support PROPFIND
 *
 * @author brad
 */
public interface ReportableResource extends Resource {

}

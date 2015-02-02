package com.bradmcevoy.http.values;

import java.util.ArrayList;

/**
 * Holds a list of href values which will be written as a list of <!supported-report-set (supported-report*)> elements
 *
 * See SupportedReportSetWriter
 *
 * @author alex
 */
public class SupportedReportSetList extends ArrayList<String> {
  private static final long serialVersionUID = 1L;
}

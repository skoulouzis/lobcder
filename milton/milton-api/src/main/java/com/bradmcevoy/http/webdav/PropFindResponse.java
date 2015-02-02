package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.Utils;
import com.bradmcevoy.http.values.ValueAndType;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

public class PropFindResponse {

    private final String href;
    private Map<QName, ValueAndType> knownProperties;
    private Map<Response.Status, List<NameAndError>> errorProperties;

    public PropFindResponse(String href, Map<QName, ValueAndType> knownProperties, Map<Response.Status, List<NameAndError>> errorProperties) {
        super();
        this.href = Utils.stripServer(href);
        this.knownProperties = knownProperties;
        this.errorProperties = errorProperties;
    }

    public String getHref() {
        return href;
    }

    public Map<QName, ValueAndType> getKnownProperties() {
        return knownProperties;
    }

    public Map<Status, List<NameAndError>> getErrorProperties() {
        return errorProperties;
    }

    /**
     * Carries the qualified name of a field in error, and an optional attribute
     * with textual information describing the error.
     *
     * This might be a validation error, for example
     *
     */
    public static class NameAndError {

        private final QName name;
        private final String error;

        public NameAndError(QName name, String error) {
            this.name = name;
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public QName getName() {
            return name;
        }
    }
}

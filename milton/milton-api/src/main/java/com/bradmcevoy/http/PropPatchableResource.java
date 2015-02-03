package com.bradmcevoy.http;

import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;

/**
 *
 *
 */
public interface PropPatchableResource extends Resource {
    /**
     *
     * @param fields
     * @deprecated - you should leave this method empty and implement CustomPropertyResource.
     * Starting with 1.5.0 you must configure a PropPatchableSetter onto the PropPatchHandler
     * for this method to be called.
     */
    public void setProperties(Fields fields);
}

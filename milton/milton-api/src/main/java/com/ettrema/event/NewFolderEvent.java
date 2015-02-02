package com.ettrema.event;

import com.bradmcevoy.http.CollectionResource;

/**
 *
 * @author brad
 */
public class NewFolderEvent implements ResourceEvent{

    private final CollectionResource collectionResource;

    public NewFolderEvent( CollectionResource collectionResource ) {
        this.collectionResource = collectionResource;
    }

    public CollectionResource getResource() {
        return collectionResource;
    }

}

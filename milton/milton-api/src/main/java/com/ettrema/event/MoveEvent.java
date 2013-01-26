package com.ettrema.event;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;


/**
 * Fired just before the resource is moved.
 *
 * @author brad
 */
public class MoveEvent implements ResourceEvent{
    private final Resource res;
	private final CollectionResource destCollection;
	private final String newName;

	/**
	 * 
	 * @param res - the resource to move
	 * @param destCollection - the destination collection
	 * @param destNewName - the name of the resource within the destination folder
	 */
    public MoveEvent( Resource res, CollectionResource destCollection, String destNewName ) {
        this.res = res;
		this.destCollection = destCollection;
		this.newName = destNewName;
    }

    @Override
    public Resource getResource() {
        return res;
    }

	public CollectionResource getDestCollection() {
		return destCollection;
	}

	public String getNewName() {
		return newName;
	}
	
	

}

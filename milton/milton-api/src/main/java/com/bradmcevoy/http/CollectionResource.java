package com.bradmcevoy.http;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.List;

/**
 * A type of Resource which can have children, ie it can act as a directory.
 * <P/>
 * This is only part of the normal behaviour of a directory though, you
 * should have a look at FolderResource for a more complete interface
 * 
 * @author brad
 */
public interface CollectionResource extends Resource {

    public Resource child(String childName) throws NotAuthorizedException, BadRequestException;
	
    List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException;
}

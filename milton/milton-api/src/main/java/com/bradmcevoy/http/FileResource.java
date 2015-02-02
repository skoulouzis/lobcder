package com.bradmcevoy.http;

/** 
 * Extends all interfaces required for typical document behavior.
 * <P/>
 * This is a good place to start if you want a normal resource. However, think
 * carefully about which interfaces to implement. Only implement those which
 * should actually be supported
 */
public interface FileResource extends CopyableResource, DeletableResource, GetableResource, MoveableResource, PostableResource, PropFindableResource {
    
}

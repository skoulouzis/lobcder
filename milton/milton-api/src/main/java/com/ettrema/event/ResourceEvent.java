package com.ettrema.event;

import com.bradmcevoy.http.Resource;

/**
 *
 * @author brad
 */
public interface ResourceEvent extends Event {
    Resource getResource();
}

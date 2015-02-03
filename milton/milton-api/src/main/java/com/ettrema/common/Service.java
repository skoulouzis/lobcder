package com.ettrema.common;

/**
 * Represents some service which can be controlled (ie started and stopped)
 *
 * @author brad
 */
public interface Service extends Stoppable {
    /**
     * Start the service. Until this is called the service should not be functional
     */
    void start();

}

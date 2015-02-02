package com.ettrema.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class EventManagerImpl implements EventManager {

    private final static Logger log = LoggerFactory.getLogger( EventManagerImpl.class );
    private final Map<Class, List<EventListener>> listenersMap = new HashMap<Class, List<EventListener>>();

    @Override
    public void fireEvent( Event e ) {
        if( log.isTraceEnabled() ) {
            log.trace( "fireEvent: " + e.getClass().getCanonicalName() );
        }
        List<EventListener> list = listenersMap.get( e.getClass() );
        if( list == null ) return;
        for( EventListener l : Collections.unmodifiableCollection(list) ) {
            if( log.isTraceEnabled() ) {
                log.trace( "  firing on: " + l.getClass() );
            }
            l.onEvent( e );
        }
    }

    @Override
    public synchronized <T extends Event> void registerEventListener( EventListener l, Class<T> c ) {
        log.info( "registerEventListener: " + l.getClass().getCanonicalName() + " - " + c.getCanonicalName() );
        List<EventListener> list = listenersMap.get( c );
        if( list == null ) {
            list = new ArrayList<EventListener>();
            listenersMap.put( c, list );
        }
        list.add( l );
    }
}

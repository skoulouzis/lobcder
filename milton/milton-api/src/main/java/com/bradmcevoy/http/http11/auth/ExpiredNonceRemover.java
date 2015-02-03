package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.HttpManager;
import com.ettrema.common.Service;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.*;

/**
 * Periodically checks a map of Nonce's to remove those which
 * have expired.
 *
 * The map should be a reference to the live map in use by a NonceProvider
 *
 * @author brad
 */
public class ExpiredNonceRemover implements Runnable, Service {

    private static final Logger log = LoggerFactory.getLogger( ExpiredNonceRemover.class );

    private static final int INTERVAL = 10;

    private final Map<UUID, Nonce> nonces;
    private final int nonceValiditySeconds;
    private final ScheduledExecutorService scheduler;

    public ExpiredNonceRemover( Map<UUID, Nonce> nonces, int nonceValiditySeconds ) {
        this.nonces = nonces;
        this.nonceValiditySeconds = nonceValiditySeconds;
        scheduler = Executors.newScheduledThreadPool( 1, new DaemonThreadFactory() );		
    }

	public void start() {
        log.debug( "scheduling checks for expired nonces every " + INTERVAL + " seconds");
        scheduler.scheduleAtFixedRate( this, 10, INTERVAL, SECONDS );
	}	
	
	public void stop() {
		scheduler.shutdown();
	}

    public void run() {
        Iterator<UUID> it = nonces.keySet().iterator();
        while( it.hasNext() ) {
            UUID key = it.next();
            Nonce n = nonces.get( key );
            if( isExpired( n.getIssued())) {
                log.debug( "removing expired nonce: " + key);
                it.remove();
            }
        }
    }

    private boolean isExpired( Date issued ) {
        long dif = (System.currentTimeMillis() - issued.getTime()) / 1000;
        return dif > nonceValiditySeconds;
    }



    private class DaemonThreadFactory implements ThreadFactory {

        public Thread newThread( Runnable r ) {
            Thread t = new Thread( r, ExpiredNonceRemover.class.getCanonicalName() );
            t.setDaemon( true );
            return t;
        }

    }
}

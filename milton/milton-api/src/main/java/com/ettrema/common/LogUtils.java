package com.ettrema.common;

import org.slf4j.Logger;


/**
 *
 * @author HP
 */
public class LogUtils {
    public static void trace(Logger log, Object ... args) {
        if( log.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for(Object o : args) {
                sb.append(o).append(", ");
            }
            log.trace(sb.toString());
        }
    }
	
    public static void debug(Logger log, Object ... args) {
        if( log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for(Object o : args) {
                sb.append(o).append(", ");
            }
            log.debug(sb.toString());
        }
    }	
	
    public static void warn(Logger log, Object ... args) {
        if( log.isWarnEnabled()) {
            StringBuilder sb = new StringBuilder();
            for(Object o : args) {
                sb.append(o).append(", ");
            }
            log.warn(sb.toString());
        }
    }		
}

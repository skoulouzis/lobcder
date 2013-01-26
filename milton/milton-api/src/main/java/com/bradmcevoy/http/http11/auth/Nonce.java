package com.bradmcevoy.http.http11.auth;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a Nonce which has been issued and is stored in memory
 *
 * @author brad
 */
public class Nonce implements Serializable{
    private static final long serialVersionUID = 1L;

    /**
     * The date it was issued.
     */
    private final UUID value;
    private final Date issued;
    private final long nonceCount;

    public Nonce( UUID value, Date issued ) {
        this.value = value;
        this.issued = issued;
        this.nonceCount = 0;
    }

    Nonce( UUID value, Date issued, long nonceCount ) {
        this.value = value;
        this.issued = issued;
        this.nonceCount = nonceCount;
    }

    public Nonce increaseNonceCount(long newNonceCount) {
//        if( newNonceCount <= this.nonceCount) throw new IllegalArgumentException( "new nonce-count is not greater then the last. old:" + nonceCount + " new:" + newNonceCount);
        return new Nonce( value, issued, newNonceCount);
    }

    /**
     * @return the value
     */
    public UUID getValue() {
        return value;
    }

    /**
     * @return the issued
     */
    public Date getIssued() {
        return issued;
    }

    public long getNonceCount() {
        return nonceCount;
    }


}

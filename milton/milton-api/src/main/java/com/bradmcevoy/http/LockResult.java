package com.bradmcevoy.http;

/**
 *
 */
public class LockResult {

    public static LockResult failed( FailureReason failureReason) {
        return new LockResult(failureReason, null);
    }

    public static LockResult success(LockToken token) {
        return new LockResult(null, token);
    }

    public enum FailureReason {
        ALREADY_LOCKED(Response.Status.SC_CONFLICT),
        PRECONDITION_FAILED(Response.Status.SC_LOCKED);

        public Response.Status status;

        FailureReason(Response.Status status) {
            this.status = status;
        }
    }

    final FailureReason failureReason;
    final LockToken lockToken;

    public LockResult(FailureReason failureReason, LockToken lockToken) {
        this.failureReason = failureReason;
        this.lockToken = lockToken;
    }

    public boolean isSuccessful() {
        return failureReason == null;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public LockToken getLockToken() {
        return lockToken;
    }

    
}

package com.bradmcevoy.http.quota;

import com.bradmcevoy.http.QuotaResource;
import com.bradmcevoy.http.Resource;

/**
 * Default implementation which just reads the quota properties from
 * QuotaResource, if the given resource implements it. Otherwise
 * returns null;
 *
 * @author brad
 */
public class DefaultQuotaDataAccessor implements QuotaDataAccessor {

    public Long getQuotaAvailable( Resource res ) {
        if( res instanceof QuotaResource ) {
            QuotaResource quotaRes = (QuotaResource) res;
            Long l = quotaRes.getQuotaAvailable();
            return l;
        } else {
            return null;
        }

    }

    public Long getQuotaUsed( Resource res ) {
        if( res instanceof QuotaResource ) {
            QuotaResource quotaRes = (QuotaResource) res;
            Long l = quotaRes.getQuotaUsed();
            return l;
        } else {
            return null;
        }
    }
}

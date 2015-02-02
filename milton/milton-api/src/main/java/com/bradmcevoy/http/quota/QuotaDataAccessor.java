package com.bradmcevoy.http.quota;

import com.bradmcevoy.http.Resource;

/**
 * Encapsulates access to quota data. The default implementation just reads
 * the properties from the resource, but other implementations might prefer
 * to use an injected service.
 *
 * @author brad
 */
public interface QuotaDataAccessor {

    Long getQuotaAvailable(Resource r);

    Long getQuotaUsed(Resource r);
}

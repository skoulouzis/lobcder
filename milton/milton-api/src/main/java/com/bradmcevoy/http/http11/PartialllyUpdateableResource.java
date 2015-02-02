package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.ReplaceableResource;
import java.io.InputStream;

/**
 * A resource which, as well as being completely replaceable, can have its content
 * partially replaced. ie individual ranges can be set
 *
 * While PutHandler will do this for you even if you don't implement this interface,
 * the approach used might not be efficient. Ie milton will retrieve your complete
 * content, then insert the update, then set the entire content back again like
 * a regular put.
 *
 * By implementing this interface you have control over how you manage the
 * updated resource.
 *
 *
 * @author brad
 */
public interface PartialllyUpdateableResource extends ReplaceableResource {
    /**
     * Update the content with the date in the given inputstream, affecting
     * only those bytes in the given range.
     *
     * Note that the range positions are zero-based, so the first byte is 0
     *
     * @param range - the range to update
     * @param in - the inputstream containing the data
     */
    void replacePartialContent(Range range, InputStream in);
}

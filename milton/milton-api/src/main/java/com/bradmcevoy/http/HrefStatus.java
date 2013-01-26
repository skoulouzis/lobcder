package com.bradmcevoy.http;

import com.bradmcevoy.http.Response.Status;

/** An immutable class to represent an item in a MultiStatus response
 *
 */
public class HrefStatus {
    public final String href;
    public final Response.Status status;

    public HrefStatus(String href, Status status) {
        this.href = href;
        this.status = status;
    }


}

package com.ettrema.http;

import com.ettrema.http.caldav.ITip;
import com.ettrema.http.caldav.ITip.StatusResponse;

/**
 *
 * @author brad
 */
public class SchedulingResponseItem {
    // Eg mailto:wilfredo@example.com
    private String recipient;

    private ITip.StatusResponse status;

    private String iCalText;

    public SchedulingResponseItem(String recipient, StatusResponse status, String iCalText) {
        this.recipient = recipient;
        this.status = status;
        this.iCalText = iCalText;
    }

    public SchedulingResponseItem() {
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public StatusResponse getStatus() {
        return status;
    }

    public void setStatus(StatusResponse status) {
        this.status = status;
    }

    public String getiCalText() {
        return iCalText;
    }

    public void setiCalText(String iCalText) {
        this.iCalText = iCalText;
    }
}

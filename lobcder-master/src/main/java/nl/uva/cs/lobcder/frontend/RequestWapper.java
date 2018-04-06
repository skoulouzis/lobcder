/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;



public class RequestWapper {

    private String method;
    private String requestURL;
    private String remoteAddr;
    private int contentLength;
    private String contentType;
    private String userNpasswd;
    private String userAgent;
    private Long timeStamp;
    private Double elapsed;

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the requestURL
     */
    public String getRequestURL() {
        return requestURL;
    }

    /**
     * @param requestURL the requestURL to set
     */
    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    /**
     * @return the remoteAddr
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * @param remoteAddr the remoteAddr to set
     */
    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    /**
     * @return the contentLength
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @param contentLength the contentLength to set
     */
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the userNpasswd
     */
    public String getUserNpasswd() {
        return userNpasswd;
    }

    /**
     * @param userNpasswd the userNpasswd to set
     */
    public void setUserNpasswd(String userNpasswd) {
        this.userNpasswd = userNpasswd;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return the timeStamp
     */
    public Long getTimeStamp() {
        return timeStamp;
    }

    /**
     * @param timeStamp the timeStamp to set
     */
    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * @return the elapsed
     */
    public Double getElapsed() {
        return elapsed;
    }

    /**
     * @param elapsed the elapsed to set
     */
    public void setElapsed(Double elapsed) {
        this.elapsed = elapsed;
    }
}

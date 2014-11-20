/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import lombok.Data;

@Data
public class RequestWapper {

    String method;
    String requestURL;
    String remoteAddr;
    int contentLength;
    String contentType;
    String userNpasswd;
    String userAgent;
    Long timeStamp;
    Double elapsed;
}

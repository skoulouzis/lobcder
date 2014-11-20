/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.optimization;

import io.milton.http.Request;
import io.milton.http.Request.Method;

/**
 *
 * @author S. Koulouzis
 */
public class LobState {

    private String resource;
    private Method method;

    public LobState(Request.Method method, String resource) {
        this.resource = resource;
        this.method = method;
    }

    public LobState() {
        
    }

    public String getResourceName() {
        return resource;
    }

    public Method getMethod() {
        return method;
    }

    public String getID() {
        return getMethod() + "," + getResourceName();
    }

    void setMethod(Method method) {
        this.method = method;
    }

    void setResourceName(String resource) {
        this.resource = resource;
    }
}

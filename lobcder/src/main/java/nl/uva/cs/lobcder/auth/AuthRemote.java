/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import nl.uva.cs.lobcder.webDav.resources.WebDataResource;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author dvasunin
 */
public class AuthRemote implements AuthI{

    private static PrincipalCacheI pc = null;
    static {
         try {
            String jndiName = "bean/PrincipalCache";
            javax.naming.Context ctx = new InitialContext();
            if (ctx == null) {
                throw new Exception("JNDI could not create InitalContext ");
            }
            javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
            pc = (PrincipalCacheI) envContext.lookup(jndiName);
        } catch (Exception ex) {
            Logger.getLogger(WebDataResource.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private String serviceURL;
    
    /**
     * @return the serviceURL
     */
    public String getServiceURL() {
        return serviceURL;
    }

    /**
     * @param serviceURL the serviceURL to set
     */
    public void setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        public String username;
        public String role[];      
    }
    
    @Override
    public MyPrincipal checkToken(String token) {
        MyPrincipal res = null;
        if(pc != null) {
            res = pc.getPrincipal(token);
        }
        if(res == null) {
            ClientConfig clientConfig = new DefaultClientConfig();
            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            Client client = Client.create(clientConfig);
            User u = client.resource(getServiceURL() + token).get(new GenericType<User> () {});
            res = new MyPrincipal(u.username, new HashSet(Arrays.asList(u.role)));
        }
        if(pc != null) {
            pc.putPrincipal(token, res);
        }
        return res;
    }
    


}

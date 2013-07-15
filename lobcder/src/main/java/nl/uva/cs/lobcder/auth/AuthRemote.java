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
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.net.ssl.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author dvasunin
 */
public class AuthRemote implements AuthI {

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
            Logger.getLogger(AuthRemote.class.getName()).log(Level.SEVERE, null, ex);
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
        try {
            if (pc != null) {
                res = pc.getPrincipal(token);
            }
            if (res == null) {
                ClientConfig clientConfig = new DefaultClientConfig();
                //  ClientConfig config = new DefaultClientConfig();
                SSLContext ctx = getSslContext();
                clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(getHostnameVerifier(), ctx));

                clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
                Client client = Client.create(clientConfig);
                User u;
//                u = client.resource(getServiceURL() + token).get(new GenericType<User>() {
//                });
                u = client.resource(getServiceURL() + token).get(new MyGenericType<User>());
                res = new MyPrincipal(u.username, new HashSet(Arrays.asList(u.role)));
                res.getRoles().add("other");
                res.getRoles().add(u.username);
            }
            if (pc != null) {
                pc.putPrincipal(token, res);
            }
        } catch (Exception e) {
        }
        return res;
    }

    private HostnameVerifier getHostnameVerifier() {
//        HostnameVerifier hv;
//        hv = new HostnameVerifier() {
//            @Override
//            public boolean verify(String string, SSLSession ssls) {
//                return true;
//            }
//        };
        return new MyHostnameVerifier();
    }

    private SSLContext getSslContext() throws Exception {
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{new MyX509TrustManager()
                }, new SecureRandom());
        return sslContext;
    }

    private static class MyGenericType<Integer> extends GenericType<User> {
    };

    private static class MyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String string, SSLSession ssls) {
            return true;
        }
    }

    private static class MyX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}

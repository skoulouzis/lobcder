/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.auth;

import lombok.Setter;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import org.apache.commons.codec.binary.Base64;

import javax.sql.DataSource;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.Level;

/**
 *
 * @author dvasunin
 */
@Log
public class AuthTicket implements AuthI {

    @Setter
    private PrincipalCacheI principalCache = null;
    @Setter
    private DataSource dataSource;

    public static class User {

        public String username;
        public String role[];
        public long validuntil;
    }

    @Override
    public MyPrincipal checkToken(String uname,String token) {
        MyPrincipal res = null;
        try {
            if (principalCache != null) {
                res = principalCache.getPrincipal(token);
            }
            if (token.length() == 12) {
                try (Connection cn = dataSource.getConnection()) {
                    try (PreparedStatement ps = cn.prepareStatement("SELECT long_tkt FROM tokens_table WHERE short_tkt = ?")) {
                        ps.setString(1, token);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                token = rs.getString(1);
                            }
                        }
                    }
                }
            }
            User u = null;
            if (res == null) {
                u = validateTicket(token);
                res = new MyPrincipal(u.username, new HashSet(Arrays.asList(u.role)), token);
                res.getRoles().add("other");
                res.getRoles().add(u.username);
                res.setValidUntil(u.validuntil);
                if (principalCache != null) {
                    principalCache.putPrincipal(token, res, u.validuntil * 1000);
                }
            }

        } catch (Exception e) {
        }
        return res;
    }
    Signature verifier = null;

    public AuthTicket() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream(PropertiesHelper.propertiesPath);
        Properties properties = new Properties();
        properties.load(in);
        String lpubKeyFile = properties.getProperty("mi.cert.pub.der", "mi_pub_key.der");
        String pubKeyType = properties.getProperty("mi.cert.alg", "DSA");
        in = classLoader.getResourceAsStream(lpubKeyFile);
        byte[] keyBytes = new byte[in.available()];
        in.read(keyBytes);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = null;
        switch (pubKeyType) {
            case "DSA":
                kf = KeyFactory.getInstance("DSA");
                verifier = Signature.getInstance("SHA1withDSA");
                break;
            case "RSA":
                kf = KeyFactory.getInstance("RSA");
                verifier = Signature.getInstance("SHA1withRSA");
                break;
        }
        PublicKey pubKey = kf.generatePublic(spec);
        verifier.initVerify(pubKey);
    }

    public User validateTicket(String token) {
        try {
            String ticket = new String(Base64.decodeBase64(token));
            byte[] signBytes;
            User u = null;

            // check errors

            Integer posSign = ticket.lastIndexOf(";sig=");

            if (posSign < 0) {
                return null;
            }

            String ticketSign = ticket.substring(posSign + 5);
            String cleanTicket = ticket.substring(0, posSign);
            signBytes = Base64.decodeBase64(ticketSign);
            verifier.update(cleanTicket.getBytes());

            if (verifier.verify(signBytes)) {

                String parts[] = cleanTicket.split(";");

                if (parts.length < 4 || parts.length > 6) {
                    return null;
                }
                u = new User();
                for (int i = 0; i < parts.length; i++) {
                    String fields[] = parts[i].split("=");
                    if (fields[0].compareTo("uid") == 0) {
                        u.username = fields[1];
                    } else if (fields[0].compareTo("validuntil") == 0) {
                        u.validuntil = Long.valueOf(fields[1]).longValue();
                    } else if (fields[0].compareTo("tokens") == 0) {
                        u.role = fields[1].split(",");
                    }
                }
                long now = new Date().getTime() / 1000;
                if (u.validuntil < now) {
                    AuthTicket.log.log(Level.FINE, "Certificate Expired");
                    return null;
                }
            } else {
                AuthTicket.log.log(Level.FINE, "Can't verify");
                return null;
            }
            return u;
        } catch (Exception e) {
            AuthTicket.log.log(Level.FINE, "Could not authenticate ticket", e);
            return null;
        }
    }

//    public static void main(String[] args) {
//        AuthI au = null;
//        try {
//            au = new AuthTicket();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

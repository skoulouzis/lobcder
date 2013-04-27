/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.writeStringToFile;

/**
 *
 * @author skoulouz
 */
public class DesEncrypter {

    private final Cipher ecipher;
    private final Cipher dcipher;
    byte[] buf = new byte[2 * 1024 * 1024];
    private Key key = null;

    public DesEncrypter(BigInteger keyInt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        byte[] iv = new byte[]{(byte) 0x8E, 0x12, 0x39, (byte) 0x9C, 0x07, 0x72, 0x6F, 0x5A};
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        ecipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

//        String keyPath = System.getProperty("user.home")+"/.lobcder/key";
//        File keyFile = new File(keyPath);
//        if (keyFile.exists()) {
//            key = loadKey(keyFile);
//        } else {
//            keyFile.getParentFile().mkdirs();
//            key = generateKey();
//            saveKey(key, keyFile);
//        }

        byte[] encoded = keyInt.toByteArray();
        key = new SecretKeySpec(encoded, "DES");

        ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
    }

    public void encrypt(InputStream in, OutputStream out) throws IOException {
        out = new CipherOutputStream(out, ecipher);

        int numRead = 0;
        while ((numRead = in.read(buf)) >= 0) {
            out.write(buf, 0, numRead);
        }
        out.close();
    }

    public void decrypt(InputStream in, OutputStream out) throws IOException {
        in = new CipherInputStream(in, dcipher);
        int numRead = 0;
        while ((numRead = in.read(buf)) >= 0) {
            out.write(buf, 0, numRead);
        }
        out.close();
    }

    public static BigInteger generateKey() throws NoSuchAlgorithmException {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
//        keyGenerator.init(256); //128 default; 192 and 256 also possible
//         byte[] encoded = key.getEncoded();
//        String data = new BigInteger(1, encoded).toString(16);
        
        return new BigInteger(1,KeyGenerator.getInstance("DES").generateKey().getEncoded());
    }

    private void saveKey(Key key, File file) throws IOException {
        byte[] encoded = key.getEncoded();
        String data = new BigInteger(1, encoded).toString(16);
        writeStringToFile(file, data);
    }

    private SecretKey loadKey(File file) throws IOException {
        String hex = new String(readFileToByteArray(file));
        byte[] encoded = new BigInteger(hex, 16).toByteArray();
        return new SecretKeySpec(encoded, "DES");
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

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
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
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
    private Key key = null;

    public DesEncrypter(BigInteger keyInt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        byte[] iv = new byte[]{(byte) 0x8E, 0x12, 0x39, (byte) 0x9C, 0x07, 0x72, 0x6F, 0x5A};
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        ecipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        dcipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

//        String keyPath = System.getProperty("user.home") + "/.lobcder/key";
//        File keyFile = new File(keyPath);
//        if (keyFile.exists()) {
//            key = loadKey(keyFile);
//        } else {
//            keyFile.getParentFile().mkdirs();
//            keyInt = generateKey();
//            byte[] encoded = keyInt.toByteArray();
//            key = new SecretKeySpec(encoded, "DES");
//            saveKey(key, keyFile);
//        }

        byte[] encoded = keyInt.toByteArray();
        key = new SecretKeySpec(encoded, "DES");

        ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
    }

    public byte[] encrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
        byte[] encVal = ecipher.doFinal(data);
        return encVal;
    }

    public void encrypt(InputStream in, OutputStream out) throws IOException {
        OutputStream cipherOut = null;
        try {
            int read;
            cipherOut = new CipherOutputStream(out, ecipher);
            byte[] copyBuffer = new byte[Constants.BUF_SIZE];
            while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                cipherOut.write(copyBuffer, 0, read);
            }
        } finally {
            try {
                in.close();
            } finally {
                cipherOut.close();
            }
        }

//        try {
//            cipherOut = new CipherOutputStream(out, ecipher);
//            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), in, cipherOut);
//            cBuff.startTransfer(new Long(-1));
//        } catch (VlException ex) {
//            throw new IOException(ex);
//        } finally {
//            if (out != null) {
//                try {
//                    out.flush();
//                    out.close();
//                } catch (java.io.IOException ex) {
//                }
//            }
//            if (in != null) {
//                in.close();
//            }
//        }
    }

    public InputStream wrapInputStream(InputStream in) {
        return new CipherInputStream(in, dcipher);
    }

    public byte[] decrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
        byte[] encVal = dcipher.doFinal(data);
        return encVal;
    }

    public void decrypt(InputStream in, OutputStream out) throws IOException {
        InputStream cipherIn = null;
        try {
            int read;
            cipherIn = new CipherInputStream(in, dcipher);
            byte[] copyBuffer = new byte[Constants.BUF_SIZE];
            while ((read = cipherIn.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                out.write(copyBuffer, 0, read);
            }
        } finally {
            try {
                cipherIn.close();
            } finally {
                out.close();
            }
        }
//        try {
//            cipherIn = new CipherInputStream(in, dcipher);
//            CircularStreamBufferTransferer cBuff = new CircularStreamBufferTransferer((Constants.BUF_SIZE), cipherIn, out);
//            cBuff.startTransfer(new Long(-1));
//        } catch (Exception ex) {
//            throw new IOException(ex);
//        } finally {
//            if (out != null) {
//                try {
//                    out.flush();
//                    out.close();
//                } catch (java.io.IOException ex) {
//                }
//            }
//            if (cipherIn != null) {
//                cipherIn.close();
//            }
//        }
    }

    public static BigInteger generateKey() throws NoSuchAlgorithmException {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
//        keyGenerator.init(56); //128 default; 192 and 256 also possible
        //        byte[] encoded = keyGenerator.generateKey().getEncoded();
        //        System.out.println("LEN: " + encoded.length);
        //        String data = new BigInteger(1, encoded).toString(16);
        //        SecretKey akey = KeyGenerator.getInstance("DES").generateKey();
        //        byte[] encoded = akey.getEncoded();
        //        BigInteger bigIntKey = new BigInteger(1, encoded);
        BigInteger bigIntKey = new BigInteger(63, 0, new Random());
        return bigIntKey;
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author S. Koulouzis
 */
public class WorkerGridHelper {

    private static Integer bufferSize;

    public static void initGlobalVFS() throws Exception {

        if (!GlobalConfig.isGlobalInitialized()) {
            copyVomsAndCerts();
            try {
                GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
            } catch (MalformedURLException ex) {
                Logger.getLogger(WorkerGridHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            // runtime configuration
            GlobalConfig.setHasUI(false);
//            GlobalConfig.setIsApplet(true);
            GlobalConfig.setPassiveMode(true);
//        GlobalConfig.setIsService(true);
            GlobalConfig.setInitURLStreamFactory(false);
            GlobalConfig.setAllowUserInteraction(false);
            GlobalConfig.setUserHomeLocation(new URL("file:///" + System.getProperty("user.home")));

//        GlobalConfig.setUsePersistantUserConfiguration(false);
            GlobalConfig.setSystemProperty("grid.proxy.location", Constants.PROXY_FILE);
            GlobalConfig.setSystemProperty("grid.certificate.location", Global.getUserHome() + "/.globus");
            GlobalConfig.setSystemProperty("grid.proxy.lifetime", "100");
//        GlobalConfig.setUsePersistantUserConfiguration(false);
            GlobalConfig.setCACertificateLocations(Constants.CERT_LOCATION);

            // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//            Global.setDebug(true);
            if (VRS.getRegistry().getVRSFactoryClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class.getName()) == null) {
                VRS.getRegistry().addVRSDriverClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class);
            }
            Global.init();
        }


        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/lobcder-worker.properties");
        Properties prop = Util.getTestProperties(in);
        in.close();
        bufferSize = Integer.valueOf(prop.getProperty(("buffer.size"), "4194304"));
    }

    public static String getProxyAsBase64String() throws FileNotFoundException, IOException {
        InputStream fis = null;
        StringBuilder sb = null;
        try {
            fis = new FileInputStream(Constants.PROXY_FILE);
            sb = new StringBuilder();

            int buffSize = bufferSize;
            if (new File(Constants.PROXY_FILE).length() < bufferSize) {
                buffSize = (int) new File(Constants.PROXY_FILE).length();
            }
            byte[] copyBuffer = new byte[buffSize];
            while ((fis.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                sb.append(new String(Base64.encodeBase64(copyBuffer)));
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return sb.toString();
    }

    public static boolean isGridProxyInt() {
        try {
            File proxyFile = new File(Constants.PROXY_FILE);
            if (!proxyFile.exists()) {
                return false;
            }
            GridProxy p = GridProxy.loadFrom(Constants.PROXY_FILE);
            return p.isValid();
        } catch (VlException ex) {
            return false;
        }
    }

    public static void initGridProxy(String vo, String proxyStringEncoded, VRSContext context, boolean destroyCert) throws Exception {
        if (context == null) {
            context = new VFSClient().getVRSContext();
        }
        GridProxy gridProxy = context.getGridProxy();
        if (destroyCert && gridProxy != null) {
            gridProxy.destroy();
            gridProxy = null;
        }
        if (gridProxy == null || !gridProxy.isValid()) {
            File proxyFile = new File(Constants.PROXY_FILE);
            byte[] proxyBytes = Base64.decodeBase64(proxyStringEncoded);
            String proxyString = new String(proxyBytes);
            FileUtils.writeStringToFile(proxyFile, proxyString);
            gridProxy = GridProxy.loadFrom(context, proxyFile.getAbsolutePath());
            context.setGridProxy(gridProxy);
            if (gridProxy.isValid() == false) {
                throw new VlException("Created Proxy is not Valid!");
            }
//            proxyFile.delete();
        }
    }

    private static void copyVomsAndCerts() throws FileNotFoundException, VlException, URISyntaxException, IOException {
        File f = new File(System.getProperty("user.home") + "/.globus/vomsdir");
        File vomsFile = new File(f.getAbsoluteFile() + "/voms.xml");
        if (!vomsFile.exists()) {
            f.mkdirs();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/voms.xml");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(vomsFile);
                int read;
                byte[] copyBuffer = new byte[bufferSize];
                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        }
        f = new File(Constants.CERT_LOCATION);
        if (!f.exists() || f.list().length == 0) {
            f.mkdirs();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL res = classLoader.getResource("/certs/");
            File sourceCerts = new File(res.toURI());
            File[] certs = sourceCerts.listFiles();
            for (File src : certs) {
                FileInputStream in = null;
                FileOutputStream out = null;
                try {
                    in = new FileInputStream(src);
                    out = new FileOutputStream(f.getAbsoluteFile() + "/" + src.getName());
                    byte[] copyBuffer = new byte[bufferSize];
                    int read;
                    while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                        out.write(copyBuffer, 0, read);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                }
            }
        }
    }
}

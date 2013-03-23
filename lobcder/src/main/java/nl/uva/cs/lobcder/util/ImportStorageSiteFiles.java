/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import com.bradmcevoy.common.Path;
import com.ettrema.http.fs.ClassPathResourceFactory;
import com.ettrema.http.fs.ClassPathResourceFactory.ClassPathResource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.VPDRI;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.bdii.BdiiService;
import nl.uva.vlet.util.bdii.StorageArea;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.VNode;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author S. koulouzis
 */
public class ImportStorageSiteFiles {

    private static int counter;

    static {
        try {
            InitGlobalVFS();
        } catch (Exception ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static VFSClient vfsClient;
    private static ArrayList<StorageArea> srms;

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(VPDRI.class.getName()).log(Level.SEVERE, null, ex);
        }
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);
        GlobalConfig.setUserHomeLocation(new URL("file:///" + System.getProperty("user.home")));
        GlobalConfig.setSystemProperty(GlobalConfig.PROP_BDII_HOSTNAME, "bdii2.grid.sara.nl:2170");
        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);
        Global.init();
    }

    private static void initVFS() throws VlException, MalformedURLException, NamingException, Exception {
        vfsClient = new VFSClient();
//        VRSContext context = vfsClient.getVRSContext();
//                BdiiService bdii = context.getBdiiService();
//        srms = bdii.getSRMv22SAsforVO("biomed");
//        
//        debug("srms: "+context.getConfigManager().getBdiiHost());
//        
//        for(StorageArea inf : srms){
//           debug("srms: "+inf.getVOStorageLocation());
//        }        
//        JDBCatalogue cat = new JDBCatalogue();
//        String resourceURI = "";
//        Credential credentials = new Credential();

//        cat.registerStorageSite(resourceURI, credentials, -1, -1, -1, -1, null);
    }

    private static void debug(String msg) {
        System.err.println(ImportStorageSiteFiles.class.getName() + ": " + msg);
    }

    public static void main(String args[]) {
        try {
            initVFS();
            importStorageSites();
        } catch (Exception ex) {
            Logger.getLogger(ImportStorageSiteFiles.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            VRS.exit();
        }
    }

    private static void importStorageSites() throws NamingException, Exception {
        String[] storageSiteURIs = new String[]{"file:///" + System.getProperty("user.home") + "/Music", "file:///" + System.getProperty("user.home") + "/Documents"};
        ArrayList<VFile> files = new ArrayList<VFile>();
//        for (String s : storageSiteURIs) {
//            walk(s, files);
//        }

        System.out.println(files.size());
        File file = new File("src/main/webapp/META-INF/context.xml");
        FileSystemResource fileResource = new FileSystemResource(file);
//        XmlBeanFactory mFactory = new XmlBeanFactory(fileResource);

        Properties env = new Properties();
//                > //  <Resource auth="Container" factory="org.apache.naming.factory.BeanFactory" name="bean/JDBCatalog"  type="nl.uva.cs.lobcder.catalogue.JDBCatalogue" />
//                env.put("auth", "Container");
        env.put("driverClassName", "com.mysql.jdbc.Driver");
        env.put("maxActive", "100");
        env.put("maxIdle", "30");
        env.put("maxWait", "10000");
        env.put("name", "jdbc/lobcder");
        env.put("password","RoomC3156");
        env.put("removeAbandoned","true");
        env.put("removeAbandonedTimeout","30");
        env.put( "type","javax.sql.DataSource");
        env.put("url","jdbc:mysql://localhost:3306/lobcder");
        env.put("username","lobcder");


        Context ctx = new InitialContext();
        String jndiName = "bean/JDBCatalog";
        if (ctx == null) {
            throw new Exception("JNDI could not create InitalContext ");
        }
        
        Context envContext = (Context) ctx.lookup("java:/comp/env");
        JDBCatalogue catalogue = (JDBCatalogue) envContext.lookup(jndiName);
        LogicalData dcd = catalogue.getResourceEntryByLDRI(Path.root, null);

        //        JDBCatalogue cat = new JDBCatalogue();
        //        createLogicalData(files, cat);
        //
        //        Credential credentials = new Credential();
        //        cat.registerStorageSite(resourceURI, credentials, -1, -1, -1, -1, null);
    }

    private static ArrayList<VFile> getFiles(String[] storageSiteURIs) throws VlException {
        for (String ssURIs : storageSiteURIs) {
            VNode n = vfsClient.openLocation(new VRL(ssURIs));
            if (n instanceof VDir) {
//                getFiles(new String[]{n.getVRL().toString()});
//                ((VDir)n).list();
                System.err.println("Dir:" + n.getVRL());
            } else {
                System.err.println("File:" + n.getVRL());
            }
        }
        return null;
    }

    public static void walk(String storageSiteURI, ArrayList<VFile> files) throws VlException {
        VDir root = vfsClient.openDir(new VRL(storageSiteURI));
        VFSNode[] list = root.list();
        for (VFSNode f : list) {
            if (f.isDir()) {
                walk(f.getVRL().toString(), files);
//                System.out.println("Dir:" + f.getVRL());
            } else {
//                counter++;
                files.add((VFile) f);
//                System.out.println("File:" + f.getVRL());
            }
        }
    }

    private static ArrayList<LogicalData> createLogicalData(ArrayList<VFile> files, JDBCatalogue cat) {
        ArrayList<LogicalData> lFiles = new ArrayList<LogicalData>();
        for (VFile f : files) {
            lFiles.add(new LogicalData(Path.path(f.getVRL().toString()), Constants.LOGICAL_FILE, cat));
        }
        return lFiles;
    }
}

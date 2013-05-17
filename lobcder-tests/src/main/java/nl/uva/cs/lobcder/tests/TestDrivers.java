/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.Global;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.io.CircularStreamBufferTransferer;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;

/**
 *
 * @author skoulouz
 */
public class TestDrivers {

    private static String vrl;
    private static String username;
    private static String password;
    private static String testURL;

    public static void main(String args[]) {
        try {
            String propBasePath = "etc" + File.separator + "test.proprties";
            Properties prop = getTestProperties(propBasePath);
            vrl = prop.getProperty("driver.test.vrl", "sftp://localhost/" + System.getProperty("user.home") + "/tmp");
            username = prop.getProperty("driver.test.username", System.getProperty("user.name"));
            password = prop.getProperty("driver.test.password");

            testURL = prop.getProperty("webdav.test.url");

            TestDrivers t = new TestDrivers();
            t.test();
        } catch (InterruptedException ex) {
            Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VlException ex) {
            Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestDrivers.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            VRS.exit();
            System.exit(0);
        }
    }

    private void test() throws VlException, FileNotFoundException, IOException, InterruptedException {
        int maxThreads = 15;
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(maxThreads);
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                maxThreads, // core thread pool size
                maxThreads, // maximum thread pool size
                20, // time to wait before resizing pool
                TimeUnit.SECONDS,
                queue,
                new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < maxThreads; i++) {
            executorService.submit(new GetTask(vrl, username, password));
        }

        executorService.shutdown();
        long sleepTime = 50;
        while (!executorService.awaitTermination(2, TimeUnit.HOURS)) {
            //            while (count >= 1) {
            int count = executorService.getActiveCount();
            sleepTime = 25 * count;
            Thread.sleep(sleepTime);
        }
    }

   

    public static Properties getTestProperties(String propPath)
            throws FileNotFoundException, IOException {
        Properties properties = new Properties();

        File f = new File(propPath);
        properties.load(new FileInputStream(f));

        return properties;
    }

   
}

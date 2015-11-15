/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author S. Koulouzis
 */
class WorkerReplicateSweep implements Runnable {

    private final File uploadDir;
    private final File cacheDir;

    public WorkerReplicateSweep() throws IOException {
        cacheDir = new File(System.getProperty("java.io.tmpdir") + File.separator + Util.getBackendWorkingFolderName());
        uploadDir = new File(cacheDir.getAbsolutePath() + File.separator + "uploads");
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

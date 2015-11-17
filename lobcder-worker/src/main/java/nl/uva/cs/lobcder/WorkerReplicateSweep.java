/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 *
 * @author S. Koulouzis
 */
@Log
class WorkerReplicateSweep implements Runnable {

    private final Catalogue cat;

    public WorkerReplicateSweep(Catalogue cat) throws IOException {
        this.cat = cat;
    }

    @Override
    public void run() {
        try {
            boolean successFlag = false;
            for (Pair<File, String> p : getFilesToReplicate()) {
                successFlag = cat.replicate(p);
                if (successFlag) {
                    successFlag = moveToCache(p.getKey());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(WorkerReplicateSweep.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private Collection<Pair<File, String>> getFilesToReplicate() throws IOException {
        Collection<Pair<File, String>> replicateList = new ArrayList<>();
        for (File f : Util.getUploadDir().listFiles()) {
            MutablePair<File, String> pair = new MutablePair<>();
            pair.setLeft(f);
            int start = f.getName().lastIndexOf("_") + 1;
            int end = f.getName().length();
            pair.setRight(f.getName().substring(start, end));
            replicateList.add(pair);
        }
        return replicateList;
    }

    private boolean moveToCache(File f) throws IOException {
        return f.renameTo(new File(Util.getCacheDir().getAbsolutePath() + File.separator + f.getName()));
    }
}

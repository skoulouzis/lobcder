/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.catalogue;

import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.delsweep.ConnectorJDBC;
import nl.uva.cs.lobcder.catalogue.delsweep.DeleteSweep;
import nl.uva.cs.lobcder.util.PropertiesHelper;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

/**
 *
 * @author S. Koulouzis
 */
@Log
class SweepersTimerTask extends TimerTask {

    private final DeleteSweep deleteSweep;
    private final ReplicateSweep replicateSweep;
    private final Boolean useRepo;
    private final Boolean useSDN;
    private WP4Sweep wp4Sweep = null;
    private SDNSweep sdnSweep = null;
//    private ThreadPoolExecutor executorService;
    private ArrayBlockingQueue<Runnable> queue;
    private int maxThreads = 5;
    private final Boolean useBulckRepo;
    private WP4SweepOLD wp4SweepOld;

    SweepersTimerTask(DataSource datasource) throws IOException, NamingException, ClassNotFoundException {
        deleteSweep = new DeleteSweep(new ConnectorJDBC(datasource, 10));
        replicateSweep = new ReplicateSweep(datasource);
        useRepo = PropertiesHelper.useMetadataRepository();
        useSDN = PropertiesHelper.useSDN();
        useBulckRepo = PropertiesHelper.useBulckMetadataRepository();
        if (useRepo && useBulckRepo) {
            wp4Sweep = new WP4Sweep(datasource);
        } else if (useRepo && !useBulckRepo) {
            wp4SweepOld = new WP4SweepOLD(datasource);
        }
        if (useSDN) {
            sdnSweep = new SDNSweep(datasource);
        }
    }

    @Override
    public void run() {
        try {
//            log.log(Level.FINE, "Start deleteSweep");
            deleteSweep.run();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, deleteSweep.getClass().getName() + " encountered and error.", e);
        } catch (Throwable e) {
            log.log(Level.SEVERE, deleteSweep.getClass().getName() + " encountered and error.", e);
        }

        try {
//            log.log(Level.FINE, "Start replicateSweep");
            replicateSweep.run();
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, replicateSweep.getClass().getName() + " encountered and error.", e);
        } catch (Throwable e) {
            log.log(Level.SEVERE, replicateSweep.getClass().getName() + " encountered and error.", e);
        }


        try {
            if (wp4Sweep != null) {
//                log.log(Level.FINE, "Start wp4Sweep");
                wp4Sweep.run();
//                initExecutor();
//                executorService.submit(wp4Sweep);
            } else if (wp4SweepOld != null) {
                wp4SweepOld.run();
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, wp4Sweep.getClass().getName() + " encountered and error.", e);
        } catch (Throwable e) {
            log.log(Level.SEVERE, wp4Sweep.getClass().getName() + " encountered and error.", e);
        }

        try {
            if (sdnSweep != null) {
                sdnSweep.run();
            }
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, sdnSweep.getClass().getName() + " encountered and error.", e);
        } catch (Throwable e) {
            log.log(Level.SEVERE, sdnSweep.getClass().getName() + " encountered and error.", e);
        }

    }

    @Override
    public boolean cancel() {
        boolean res = super.cancel();
//        executorService.shutdown();
//        replicateSweep.stop();
        return res;

    }

    private void initExecutor() {
        if (queue == null) {
            queue = new ArrayBlockingQueue<>(maxThreads);
        }
//        if (executorService == null) {
//            executorService = new ThreadPoolExecutor(
//                    maxThreads, // core thread pool size
//                    maxThreads, // maximum thread pool size
//                    1, // time to wait before resizing pool
//                    TimeUnit.MINUTES,
//                    queue,
//                    new ThreadPoolExecutor.CallerRunsPolicy());
//        }
    }
}

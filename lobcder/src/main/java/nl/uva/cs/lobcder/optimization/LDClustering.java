/*
 * Copyright 2014 S. Koulouzis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.uva.cs.lobcder.optimization;

import be.abeel.util.Pair;
import io.milton.common.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import libsvm.LibSVM;
import lombok.extern.java.Log;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.evaluation.EvaluateDataset;
import net.sf.javaml.clustering.AQBC;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.Cobweb;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.clustering.FarthestFirst;
import net.sf.javaml.clustering.IterativeFarthestFirst;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.clustering.KMedoids;
import net.sf.javaml.clustering.SOM;
import net.sf.javaml.clustering.evaluation.AICScore;
import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.clustering.evaluation.SumOfSquaredErrors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SparseInstance;
import net.sf.javaml.featureselection.scoring.GainRatio;
import net.sf.javaml.filter.normalize.InstanceNormalizeMidrange;
import net.sf.javaml.filter.normalize.NormalizeMean;
import net.sf.javaml.filter.normalize.NormalizeMidrange;
import net.sf.javaml.sampling.Sampling;
import net.sf.javaml.tools.InstanceTools;
import net.sf.javaml.tools.weka.WekaClusterer;
import nl.uva.cs.lobcder.resources.LogicalData;
import org.apache.commons.dbcp.BasicDataSource;
import weka.clusterers.CLOPE;
import weka.clusterers.EM;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.MakeDensityBasedClusterer;
import weka.clusterers.RandomizableClusterer;
import weka.clusterers.RandomizableDensityBasedClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.clusterers.sIB;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Resample;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClustering implements Runnable {

    private static Dataset fileDataset;
    private static Dataset[] fileClusters;
    private final BasicDataSource dataSource;

    public LDClustering() throws NamingException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        dataSource = new BasicDataSource();

        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername("lobcder");
        dataSource.setPassword("RoomC3156");
        String url = "jdbc:mysql://localhost:3306/lobcderDB2?zeroDateTimeBehavior=convertToNull";
        dataSource.setUrl(url);
        dataSource.setMaxActive(10);
        dataSource.setMaxIdle(5);
        dataSource.setInitialSize(5);
        dataSource.setValidationQuery("SELECT 1");

//        DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
//        
//        datasource2 = DriverManager.getConnection(url);
//        datasource2.setAutoCommit(false);

        fileDataset = new DefaultDataset();


//        initAttributes();



    }

    public Connection getConnection() throws SQLException {
        Connection cn = dataSource.getConnection();
        cn.setAutoCommit(false);
        return cn;
    }

    public static void main(String args[]) throws Exception {
        LDClustering c = new LDClustering();
        c.run();
    }

    @Override
    public void run() {
        try {
            buildOrUpdateDataset();
//            printDataset();
            featureScoring();
            normalizeDataset();
            sample();
//            featureScoring();
            cluster();
            printClusters();
            evaluateCluster();
        } catch (SQLException ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void buildOrUpdateDataset() throws SQLException {
        try (Connection connection = getConnection()) {
            LogicalData root = getLogicalDataByUid(Long.valueOf(1), connection);
            addFeatures(Path.root, root, connection, null);
        }
    }

    private LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(UID);
                res.setParentRef(rs.getLong(1));
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getString(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));
                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    private void addFeatures(Path p, LogicalData node, Connection connection, ArrayList<Path> nodes) throws SQLException {
        Collection<LogicalData> children = getChildrenByParentRef(node.getUid(), connection);
        for (LogicalData n : children) {
            ArrayList<Double> featuresList = new ArrayList<>();

            Long cDate = n.getCreateDate();
            featuresList.add(Double.valueOf(cDate));

            Long vDate = n.getLastValidationDate();
            if (vDate == null) {
                vDate = Long.valueOf(0);
            }

            featuresList.add(Double.valueOf(vDate));
            Long len = n.getLength();
            if (len == null) {
                len = Long.valueOf(0);
            }
            featuresList.add(Double.valueOf(len));


            Long lTimeout = n.getLockTimeout();
            if (lTimeout == null) {
                lTimeout = Long.valueOf(0);
            }
            featuresList.add(Double.valueOf(lTimeout));


            Long mDate = n.getModifiedDate();
            if (mDate == null) {
                mDate = Long.valueOf(0);
            }
            featuresList.add(Double.valueOf(mDate));

            featuresList.add(Double.valueOf(n.getParentRef()));
//            instance.setValue(parentRefAttribute, n.getParentRef());

            featuresList.add(Double.valueOf(n.getPdriGroupId()));
//            instance.setValue(pdriGroupIdAttribute, n.getPdriGroupId());

            featuresList.add(Double.valueOf(n.getUid()));
//            instance.setValue(uidAttribute, n.getUid());

            String feature = n.getContentTypesAsString();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getDataLocationPreference();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getDescription();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getLockDepth();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getLockScope();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getLockTokenID();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getLockType();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            feature = n.getLockedByUser();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }
            featuresList.add(toAscii(feature));

            String name = n.getName();
            if (name.length() <= 0) {
                name = "/";
            }
            featuresList.add(toAscii(name));


            String owner = n.getOwner();
            featuresList.add(toAscii(owner));

            feature = n.getStatus();
            if (feature == null || feature.length() <= 0) {
                feature = "NON";
            }

            Boolean isSupervised = n.getSupervised();
            if (isSupervised) {
                featuresList.add(1.0);
            } else {
                featuresList.add(0.0);
            }

            String type = n.getType();
            featuresList.add(toAscii(type));


            double[] featuresArray = new double[featuresList.size()];
            for (int i = 0; i < featuresArray.length; i++) {
                featuresArray[i] = featuresList.get(i);
//                log.log(Level.INFO, "featuresArray[{0}]: {1}", new Object[]{i, featuresArray[i]});
            }
            Instance instance = new DenseInstance(featuresArray, n.getName());
//            log.log(Level.INFO, "Add instance: {0}", n.getName());
//            fileDataset.addElement(instance);
            fileDataset.add(instance);

            if (n.getUid() != node.getUid()) {
//                log.log(Level.INFO, "children: " + ld.getName());
                Path nextPath = Path.path(p, n.getName());
//                log.log(Level.INFO, "node: " + nextPath);
                if (n.isFolder()) {
                    addFeatures(nextPath, n, connection, nodes);
                }
            }
        }
    }

    private Collection<LogicalData> getChildrenByParentRef(Long parentRef, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid, ownerId, datatype, ldName, createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                + "description, locationPreference "
                + "FROM ldata_table WHERE ldata_table.parentRef = ?")) {
            preparedStatement.setLong(1, parentRef);
            ResultSet rs = preparedStatement.executeQuery();
            LinkedList<LogicalData> res = new LinkedList<>();
            while (rs.next()) {
                LogicalData element = new LogicalData();
                element.setUid(rs.getLong(1));
                element.setParentRef(parentRef);
                element.setOwner(rs.getString(2));
                element.setType(rs.getString(3));
                element.setName(rs.getString(4));
                element.setCreateDate(rs.getTimestamp(5).getTime());
                element.setModifiedDate(rs.getTimestamp(6).getTime());
                element.setLength(rs.getLong(7));
                element.setContentTypesAsString(rs.getString(8));
                element.setPdriGroupId(rs.getLong(9));
                element.setSupervised(rs.getBoolean(10));
                element.setChecksum(rs.getString(11));
                element.setLastValidationDate(rs.getLong(12));
                element.setLockTokenID(rs.getString(13));
                element.setLockScope(rs.getString(14));
                element.setLockType(rs.getString(15));
                element.setLockedByUser(rs.getString(16));
                element.setLockDepth(rs.getString(17));
                element.setLockTimeout(rs.getLong(18));
                element.setDescription(rs.getString(19));
                element.setDataLocationPreference(rs.getString(20));
                res.add(element);
            }
            return res;
        }
    }

    private static void cluster() {
//        Clusterer clusterer = new KMeans();
//        Clusterer clusterer = new AQBC();
        Clusterer clusterer = new Cobweb();
//        Clusterer clusterer = new DensityBasedSpatialClustering();
//        Clusterer clusterer = new FarthestFirst();
//        Clusterer clusterer = new KMedoids();
//        Clusterer clusterer = new OPTICS();
//        Clusterer clusterer = new SOM();

//        XMeans xm = new XMeans();
//        SimpleKMeans xm = new SimpleKMeans();
//        CLOPE xm = new CLOPE();
//        weka.clusterers.EM xm = new EM();
//        weka.clusterers.FarthestFirst xm = new weka.clusterers.FarthestFirst();
//        weka.clusterers.FilteredClusterer xm = new FilteredClusterer();
//        weka.clusterers.HierarchicalClusterer xm = new HierarchicalClusterer();
//        weka.clusterers.MakeDensityBasedClusterer xm = new MakeDensityBasedClusterer();
//        weka.clusterers.sIB xm = new sIB();


        /* Wrap Weka clusterer in bridge */


//        Clusterer clusterer = new WekaClusterer(xm);
        fileClusters = clusterer.cluster(fileDataset);

    }

    private static void printClusters() {
        for (int i = 0; i < fileClusters.length; i++) {
            log.log(Level.INFO, "Cluster: {0}", i);
            Dataset ds = fileClusters[i];

//            Iterator<Instance> iter = ds.iterator();

//            while (iter.hasNext()) {
//                Instance inst = iter.next();
//                int id = inst.getID();
//                log.log(Level.INFO, "\t------------------ ");
//                log.log(Level.INFO, "\tID: {0}", id);
//                Object classValue = inst.classValue();
//                if (classValue != null) {
//                    log.log(Level.INFO, "\tclassValue: {0}", classValue);
//                }
//                SortedSet<Integer> keys = inst.keySet();
//                Iterator<Integer> kIter = keys.iterator();
//                while (kIter.hasNext()) {
//                    Integer key = kIter.next();
//                    Double val = inst.get(key);
//                    log.log(Level.INFO, "key: " + key + " val: " + val);
//                }
//                log.log(Level.INFO, "\t------------------ ");
//            }
        }
    }

    private static Double toAscii(String s) {
        StringBuilder sb = new StringBuilder();
//        String ascString = null;
//        long asciiInt;
        for (int i = 0; i < s.length(); i++) {
            sb.append((int) s.charAt(i));
//            char c = s.charAt(i);
        }
//        ascString = sb.toString();
//        asciiInt = Long.parseLong(ascString);
        return Double.valueOf(sb.toString());
    }

    private static void featureScoring() {
        /* Create a feature scoring algorithm */
        GainRatio ga = new GainRatio();
        /* Apply the algorithm to the data set */
        ga.build(fileDataset);
        System.out.println("noAttributes: " + ga.noAttributes());
        for (int i = 0; i < ga.noAttributes(); i++) {
            System.out.println("score[" + i + "] " + ga.score(i));
        }
    }

    private static void normalizeDataset() {


        Instance rgB = fileDataset.get(10);
        int id = rgB.getID();
        System.out.println("------------------ ");
        System.out.println("ID: " + id);
        Object classValue = rgB.classValue();
        if (classValue != null) {
            System.out.println("classValue: " + classValue.getClass().getName());
            System.out.println("classValue: " + classValue);
        }
        SortedSet<Integer> keys = rgB.keySet();
        Iterator<Integer> kIter = keys.iterator();
        while (kIter.hasNext()) {
            Integer key = kIter.next();
            Double val = rgB.get(key);
            System.out.println("key: " + key + " val: " + val);
        }
        System.out.println("------------------ ");

//        NormalizeMean nmr = new NormalizeMean();
//        nmr.build(fileDataset);
//        nmr.filter(fileDataset);
        InstanceNormalizeMidrange inm = new InstanceNormalizeMidrange(0.5, 1);
        inm.build(fileDataset);
        inm.filter(fileDataset);


        rgB = fileDataset.get(10);
        id = rgB.getID();
        System.out.println("------------------ ");
        System.out.println("ID: " + id);
        classValue = rgB.classValue();
        if (classValue != null) {
            System.out.println("classValue: " + classValue.getClass().getName());
            System.out.println("classValue: " + classValue);
        }
        keys = rgB.keySet();
        kIter = keys.iterator();
        while (kIter.hasNext()) {
            Integer key = kIter.next();
            Double val = rgB.get(key);
            System.out.println("key: " + key + " val: " + val);
        }
        System.out.println("------------------ ");
    }

    private void initAttributes() {
//        String types = "application/andrew-inset,"
//                       + "application/applixware,"
//                       + "application/atom+xml,"
//                       + "application/atomcat+xml,"
//                       + "application/atomsvc+xml,"
//                       + "application/ccxml+xml,"
//                       + "application/cdmi-capability,"
//                       + "application/cdmi-container,"
//                       + "application/cdmi-domain,"
//                       + "application/cdmi-object,"
//                       + "application/cdmi-queue,"
//                       + "application/cu-seeme,"
//                       + "application/davmount+xml,"
//                       + "application/dssc+der,"
//                       + "application/dssc+xml,"
//                       + "application/ecmascript,"
//                       + "application/emma+xml,"
//                       + "application/epub+zip,"
//                       + "application/exi,"
//                       + "application/font-tdpfr,"
//                       + "application/hyperstudio,"
//                       + "application/ipfix,"
//                       + "application/java-archive,"
//                       + "application/java-serialized-object,"
//                       + "application/java-vm,"
//                       + "application/javascript,"
//                       + "application/json,"
//                       + "application/mac-binhex40,"
//                       + "application/mac-compactpro,"
//                       + "application/mads+xml,"
//                       + "application/marc,"
//                       + "application/marcxml+xml,"
//                       + "application/mathematica,"
//                       + "application/mathml+xml,"
//                       + "application/mbox,"
//                       + "application/mediaservercontrol+xml,"
//                       + "application/metalink4+xml,"
//                       + "application/mets+xml,"
//                       + "application/mods+xml,"
//                       + "application/mp21,"
//                       + "application/mp4,"
//                       + "application/msword,"
//                       + "application/mxf,"
//                       + "application/octet-stream,"
//                       + "application/oda,"
//                       + "application/oebps-package+xml,"
//                       + "application/ogg,"
//                       + "application/onenote,"
//                       + "application/patch-ops-error+xml,"
//                       + "application/pdf,"
//                       + "application/pgp-encrypted,"
//                       + "application/pgp-signature,"
//                       + "application/pics-rules,"
//                       + "application/pkcs10,"
//                       + "application/pkcs7-mime,"
//                       + "application/pkcs7-signature,"
//                       + "application/pkcs8,"
//                       + "application/pkix-attr-cert,"
//                       + "application/pkix-cert,"
//                       + "application/pkix-crl,"
//                       + "application/pkix-pkipath,"
//                       + "application/pkixcmp,"
//                       + "application/pls+xml,"
//                       + "application/postscript,"
//                       + "application/prs.cww,"
//                       + "application/pskc+xml,"
//                       + "application/rdf+xml,"
//                       + "application/reginfo+xml,"
//                       + "application/relax-ng-compact-syntax,"
//                       + "application/resource-lists+xml,"
//                       + "application/resource-lists-diff+xml,"
//                       + "application/rls-services+xml,"
//                       + "application/rsd+xml,"
//                       + "application/rss+xml,"
//                       + "application/rtf,"
//                       + "application/sbml+xml,"
//                       + "application/scvp-cv-request,"
//                       + "application/scvp-cv-response,"
//                       + "application/scvp-vp-request,"
//                       + "application/scvp-vp-response,"
//                       + "application/sdp,"
//                       + "application/set-payment-initiation,"
//                       + "application/set-registration-initiation,"
//                       + "application/shf+xml,"
//                       + "application/smil+xml,"
//                       + "application/sparql-query,"
//                       + "application/sparql-results+xml,"
//                       + "application/srgs,"
//                       + "application/srgs+xml,"
//                       + "application/sru+xml,"
//                       + "application/ssml+xml,"
//                       + "application/tei+xml,"
//                       + "application/thraud+xml,"
//                       + "application/timestamped-data,"
//                       + "application/vnd.3gpp.pic-bw-large,"
//                       + "application/vnd.3gpp.pic-bw-small,"
//                       + "application/vnd.3gpp.pic-bw-var,"
//                       + "application/vnd.3gpp2.tcap,"
//                       + "application/vnd.3m.post-it-notes,"
//                       + "application/vnd.accpac.simply.aso,"
//                       + "application/vnd.accpac.simply.imp,"
//                       + "application/vnd.acucobol,"
//                       + "application/vnd.acucorp,"
//                       + "application/vnd.adobe.air-application-installer-package+zip,"
//                       + "application/vnd.adobe.fxp,"
//                       + "application/vnd.adobe.xdp+xml,"
//                       + "application/vnd.adobe.xfdf,"
//                       + "application/vnd.ahead.space,"
//                       + "application/vnd.airzip.filesecure.azf,"
//                       + "application/vnd.airzip.filesecure.azs,"
//                       + "application/vnd.amazon.ebook,"
//                       + "application/vnd.americandynamics.acc,"
//                       + "application/vnd.amiga.ami,"
//                       + "application/vnd.android.package-archive,"
//                       + "application/vnd.anser-web-certificate-issue-initiation,"
//                       + "application/vnd.anser-web-funds-transfer-initiation,"
//                       + "application/vnd.antix.game-component,"
//                       + "application/vnd.apple.installer+xml,"
//                       + "application/vnd.apple.mpegurl,"
//                       + "application/vnd.aristanetworks.swi,"
//                       + "application/vnd.audiograph,"
//                       + "application/vnd.blueice.multipass,"
//                       + "application/vnd.bmi,"
//                       + "application/vnd.businessobjects,"
//                       + "application/vnd.chemdraw+xml,"
//                       + "application/vnd.chipnuts.karaoke-mmd,"
//                       + "application/vnd.cinderella,"
//                       + "application/vnd.claymore,"
//                       + "application/vnd.cloanto.rp9,"
//                       + "application/vnd.clonk.c4group,"
//                       + "application/vnd.cluetrust.cartomobile-config,"
//                       + "application/vnd.cluetrust.cartomobile-config-pkg,"
//                       + "application/vnd.commonspace,"
//                       + "application/vnd.contact.cmsg,"
//                       + "application/vnd.cosmocaller,"
//                       + "application/vnd.crick.clicker,"
//                       + "application/vnd.crick.clicker.keyboard,"
//                       + "application/vnd.crick.clicker.palette,"
//                       + "application/vnd.crick.clicker.template,"
//                       + "application/vnd.crick.clicker.wordbank,"
//                       + "application/vnd.criticaltools.wbs+xml,"
//                       + "application/vnd.ctc-posml,"
//                       + "application/vnd.cups-ppd,"
//                       + "application/vnd.curl.car,"
//                       + "application/vnd.curl.pcurl,"
//                       + "application/vnd.data-vision.rdz,"
//                       + "application/vnd.denovo.fcselayout-link,"
//                       + "application/vnd.dna,"
//                       + "application/vnd.dolby.mlp,"
//                       + "application/vnd.dpgraph,"
//                       + "application/vnd.dreamfactory,"
//                       + "application/vnd.dvb.ait,"
//                       + "application/vnd.dvb.service,"
//                       + "application/vnd.dynageo,"
//                       + "application/vnd.ecowin.chart,"
//                       + "application/vnd.enliven,"
//                       + "application/vnd.epson.esf,"
//                       + "application/vnd.epson.msf,"
//                       + "application/vnd.epson.quickanime,"
//                       + "application/vnd.epson.salt,"
//                       + "application/vnd.epson.ssf,"
//                       + "application/vnd.eszigno3+xml,"
//                       + "application/vnd.ezpix-album,"
//                       + "application/vnd.ezpix-package,"
//                       + "application/vnd.fdf,"
//                       + "application/vnd.fdsn.seed,"
//                       + "application/vnd.flographit,"
//                       + "application/vnd.fluxtime.clip,"
//                       + "application/vnd.framemaker,"
//                       + "application/vnd.frogans.fnc,"
//                       + "application/vnd.frogans.ltf,"
//                       + "application/vnd.fsc.weblaunch,"
//                       + "application/vnd.fujitsu.oasys,"
//                       + "application/vnd.fujitsu.oasys2,"
//                       + "application/vnd.fujitsu.oasys3,"
//                       + "application/vnd.fujitsu.oasysgp,"
//                       + "application/vnd.fujitsu.oasysprs,"
//                       + "application/vnd.fujixerox.ddd,"
//                       + "application/vnd.fujixerox.docuworks,"
//                       + "application/vnd.fujixerox.docuworks.binder,"
//                       + "application/vnd.fuzzysheet,"
//                       + "application/vnd.genomatix.tuxedo,"
//                       + "application/vnd.geogebra.file,"
//                       + "application/vnd.geogebra.tool,"
//                       + "application/vnd.geometry-explorer,"
//                       + "application/vnd.geonext,"
//                       + "application/vnd.geoplan,"
//                       + "application/vnd.geospace,"
//                       + "application/vnd.gmx,"
//                       + "application/vnd.google-earth.kml+xml,"
//                       + "application/vnd.google-earth.kmz,"
//                       + "application/vnd.grafeq,"
//                       + "application/vnd.groove-account,"
//                       + "application/vnd.groove-help,"
//                       + "application/vnd.groove-identity-message,"
//                       + "application/vnd.groove-injector,"
//                       + "application/vnd.groove-tool-message,"
//                       + "application/vnd.groove-tool-template,"
//                       + "application/vnd.groove-vcard,"
//                       + "application/vnd.hal+xml,"
//                       + "application/vnd.handheld-entertainment+xml,"
//                       + "application/vnd.hbci,"
//                       + "application/vnd.hhe.lesson-player,"
//                       + "application/vnd.hp-hpgl,"
//                       + "application/vnd.hp-hpid,"
//                       + "application/vnd.hp-hps,"
//                       + "application/vnd.hp-jlyt,"
//                       + "application/vnd.hp-pcl,"
//                       + "application/vnd.hp-pclxl,"
//                       + "application/vnd.hydrostatix.sof-data,"
//                       + "application/vnd.hzn-3d-crossword,"
//                       + "application/vnd.ibm.minipay,"
//                       + "application/vnd.ibm.modcap,"
//                       + "application/vnd.ibm.rights-management,"
//                       + "application/vnd.ibm.secure-container,"
//                       + "application/vnd.iccprofile,"
//                       + "application/vnd.igloader,"
//                       + "application/vnd.immervision-ivp,"
//                       + "application/vnd.immervision-ivu,"
//                       + "application/vnd.insors.igm,"
//                       + "application/vnd.intercon.formnet,"
//                       + "application/vnd.intergeo,"
//                       + "application/vnd.intu.qbo,"
//                       + "application/vnd.intu.qfx,"
//                       + "application/vnd.ipunplugged.rcprofile,"
//                       + "application/vnd.irepository.package+xml,"
//                       + "application/vnd.is-xpr,"
//                       + "application/vnd.isac.fcs,"
//                       + "application/vnd.jam,"
//                       + "application/vnd.jcp.javame.midlet-rms,"
//                       + "application/vnd.jisp,"
//                       + "application/vnd.joost.joda-archive,"
//                       + "application/vnd.kahootz,"
//                       + "application/vnd.kde.karbon,"
//                       + "application/vnd.kde.kchart,"
//                       + "application/vnd.kde.kformula,"
//                       + "application/vnd.kde.kivio,"
//                       + "application/vnd.kde.kontour,"
//                       + "application/vnd.kde.kpresenter,"
//                       + "application/vnd.kde.kspread,"
//                       + "application/vnd.kde.kword,"
//                       + "application/vnd.kenameaapp,"
//                       + "application/vnd.kidspiration,"
//                       + "application/vnd.kinar,"
//                       + "application/vnd.koan,"
//                       + "application/vnd.kodak-descriptor,"
//                       + "application/vnd.las.las+xml,"
//                       + "application/vnd.llamagraphics.life-balance.desktop,"
//                       + "application/vnd.llamagraphics.life-balance.exchange+xml,"
//                       + "application/vnd.lotus-1-2-3,"
//                       + "application/vnd.lotus-approach,"
//                       + "application/vnd.lotus-freelance,"
//                       + "application/vnd.lotus-notes,"
//                       + "application/vnd.lotus-organizer,"
//                       + "application/vnd.lotus-screencam,"
//                       + "application/vnd.lotus-wordpro,"
//                       + "application/vnd.macports.portpkg,"
//                       + "application/vnd.mcd,"
//                       + "application/vnd.medcalcdata,"
//                       + "application/vnd.mediastation.cdkey,"
//                       + "application/vnd.mfer,"
//                       + "application/vnd.mfmp,"
//                       + "application/vnd.micrografx.flo,"
//                       + "application/vnd.micrografx.igx,"
//                       + "application/vnd.mif,"
//                       + "application/vnd.mobius.daf,"
//                       + "application/vnd.mobius.dis,"
//                       + "application/vnd.mobius.mbk,"
//                       + "application/vnd.mobius.mqy,"
//                       + "application/vnd.mobius.msl,"
//                       + "application/vnd.mobius.plc,"
//                       + "application/vnd.mobius.txf,"
//                       + "application/vnd.mophun.application,"
//                       + "application/vnd.mophun.certificate,"
//                       + "application/vnd.mozilla.xul+xml,"
//                       + "application/vnd.ms-artgalry,"
//                       + "application/vnd.ms-cab-compressed,"
//                       + "application/vnd.ms-excel,"
//                       + "application/vnd.ms-excel.addin.macroenabled.12,"
//                       + "application/vnd.ms-excel.sheet.binary.macroenabled.12,"
//                       + "application/vnd.ms-excel.sheet.macroenabled.12,"
//                       + "application/vnd.ms-excel.template.macroenabled.12,"
//                       + "application/vnd.ms-fontobject,"
//                       + "application/vnd.ms-htmlhelp,"
//                       + "application/vnd.ms-ims,"
//                       + "application/vnd.ms-lrm,"
//                       + "application/vnd.ms-officetheme,"
//                       + "application/vnd.ms-pki.seccat,"
//                       + "application/vnd.ms-pki.stl,"
//                       + "application/vnd.ms-powerpoint,"
//                       + "application/vnd.ms-powerpoint.addin.macroenabled.12,"
//                       + "application/vnd.ms-powerpoint.presentation.macroenabled.12,"
//                       + "application/vnd.ms-powerpoint.slide.macroenabled.12,"
//                       + "application/vnd.ms-powerpoint.slideshow.macroenabled.12,"
//                       + "application/vnd.ms-powerpoint.template.macroenabled.12,"
//                       + "application/vnd.ms-project,"
//                       + "application/vnd.ms-word.document.macroenabled.12,"
//                       + "application/vnd.ms-word.template.macroenabled.12,"
//                       + "application/vnd.ms-works,"
//                       + "application/vnd.ms-wpl,"
//                       + "application/vnd.ms-xpsdocument,"
//                       + "application/vnd.mseq,"
//                       + "application/vnd.musician,"
//                       + "application/vnd.muvee.style,"
//                       + "application/vnd.neurolanguage.nlu,"
//                       + "application/vnd.noblenet-directory,"
//                       + "application/vnd.noblenet-sealer,"
//                       + "application/vnd.noblenet-web,"
//                       + "application/vnd.nokia.n-gage.data,"
//                       + "application/vnd.nokia.n-gage.symbian.install,"
//                       + "application/vnd.nokia.radio-preset,"
//                       + "application/vnd.nokia.radio-presets,"
//                       + "application/vnd.novadigm.edm,"
//                       + "application/vnd.novadigm.edx,"
//                       + "application/vnd.novadigm.ext,"
//                       + "application/vnd.oasis.opendocument.chart,"
//                       + "application/vnd.oasis.opendocument.chart-template,"
//                       + "application/vnd.oasis.opendocument.database,"
//                       + "application/vnd.oasis.opendocument.formula,"
//                       + "application/vnd.oasis.opendocument.formula-template,"
//                       + "application/vnd.oasis.opendocument.graphics,"
//                       + "application/vnd.oasis.opendocument.graphics-template,"
//                       + "application/vnd.oasis.opendocument.image,"
//                       + "application/vnd.oasis.opendocument.image-template,"
//                       + "application/vnd.oasis.opendocument.presentation,"
//                       + "application/vnd.oasis.opendocument.presentation-template,"
//                       + "application/vnd.oasis.opendocument.spreadsheet,"
//                       + "application/vnd.oasis.opendocument.spreadsheet-template,"
//                       + "application/vnd.oasis.opendocument.text,"
//                       + "application/vnd.oasis.opendocument.text-master,"
//                       + "application/vnd.oasis.opendocument.text-template,"
//                       + "application/vnd.oasis.opendocument.text-web,"
//                       + "application/vnd.olpc-sugar,"
//                       + "application/vnd.oma.dd2+xml,"
//                       + "application/vnd.openofficeorg.extension,"
//                       + "application/vnd.openxmlformats-officedocument.presentationml.presentation,"
//                       + "application/vnd.openxmlformats-officedocument.presentationml.slide,"
//                       + "application/vnd.openxmlformats-officedocument.presentationml.slideshow,"
//                       + "application/vnd.openxmlformats-officedocument.presentationml.template,"
//                       + "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,"
//                       + "application/vnd.openxmlformats-officedocument.spreadsheetml.template,"
//                       + "application/vnd.openxmlformats-officedocument.wordprocessingml.document,"
//                       + "application/vnd.openxmlformats-officedocument.wordprocessingml.template,"
//                       + "application/vnd.osgeo.mapguide.package,"
//                       + "application/vnd.osgi.dp,"
//                       + "application/vnd.palm,"
//                       + "application/vnd.pawaafile,"
//                       + "application/vnd.pg.format,"
//                       + "application/vnd.pg.osasli,"
//                       + "application/vnd.picsel,"
//                       + "application/vnd.pmi.widget,"
//                       + "application/vnd.pocketlearn,"
//                       + "application/vnd.powerbuilder6,"
//                       + "application/vnd.previewsystems.box,"
//                       + "application/vnd.proteus.magazine,"
//                       + "application/vnd.publishare-delta-tree,"
//                       + "application/vnd.pvi.ptid1,"
//                       + "application/vnd.quark.quarkxpress,"
//                       + "application/vnd.realvnc.bed,"
//                       + "application/vnd.recordare.musicxml,"
//                       + "application/vnd.recordare.musicxml+xml,"
//                       + "application/vnd.rig.cryptonote,"
//                       + "application/vnd.rim.cod,"
//                       + "application/vnd.rn-realmedia,"
//                       + "application/vnd.route66.link66+xml,"
//                       + "application/vnd.sailingtracker.track,"
//                       + "application/vnd.seemail,"
//                       + "application/vnd.sema,"
//                       + "application/vnd.semd,"
//                       + "application/vnd.semf,"
//                       + "application/vnd.shana.informed.formdata,"
//                       + "application/vnd.shana.informed.formtemplate,"
//                       + "application/vnd.shana.informed.interchange,"
//                       + "application/vnd.shana.informed.package,"
//                       + "application/vnd.simtech-mindmapper,"
//                       + "application/vnd.smaf,"
//                       + "application/vnd.smart.teacher,"
//                       + "application/vnd.solent.sdkm+xml,"
//                       + "application/vnd.spotfire.dxp,"
//                       + "application/vnd.spotfire.sfs,"
//                       + "application/vnd.stardivision.calc,"
//                       + "application/vnd.stardivision.draw,"
//                       + "application/vnd.stardivision.impress,"
//                       + "application/vnd.stardivision.math,"
//                       + "application/vnd.stardivision.writer,"
//                       + "application/vnd.stardivision.writer-global,"
//                       + "application/vnd.stepmania.stepchart,"
//                       + "application/vnd.sun.xml.calc,"
//                       + "application/vnd.sun.xml.calc.template,"
//                       + "application/vnd.sun.xml.draw,"
//                       + "application/vnd.sun.xml.draw.template,"
//                       + "application/vnd.sun.xml.impress,"
//                       + "application/vnd.sun.xml.impress.template,"
//                       + "application/vnd.sun.xml.math,"
//                       + "application/vnd.sun.xml.writer,"
//                       + "application/vnd.sun.xml.writer.global,"
//                       + "application/vnd.sun.xml.writer.template,"
//                       + "application/vnd.sus-calendar,"
//                       + "application/vnd.svd,"
//                       + "application/vnd.symbian.install,"
//                       + "application/vnd.syncml+xml,"
//                       + "application/vnd.syncml.dm+wbxml,"
//                       + "application/vnd.syncml.dm+xml,"
//                       + "application/vnd.tao.intent-module-archive,"
//                       + "application/vnd.tmobile-livetv,"
//                       + "application/vnd.trid.tpt,"
//                       + "application/vnd.triscape.mxs,"
//                       + "application/vnd.trueapp,"
//                       + "application/vnd.ufdl,"
//                       + "application/vnd.uiq.theme,"
//                       + "application/vnd.umajin,"
//                       + "application/vnd.unity,"
//                       + "application/vnd.uoml+xml,"
//                       + "application/vnd.vcx,"
//                       + "application/vnd.visio,"
//                       + "application/vnd.visionary,"
//                       + "application/vnd.vsf,"
//                       + "application/vnd.wap.wbxml,"
//                       + "application/vnd.wap.wmlc,"
//                       + "application/vnd.wap.wmlscriptc,"
//                       + "application/vnd.webturbo,"
//                       + "application/vnd.wolfram.player,"
//                       + "application/vnd.wordperfect,"
//                       + "application/vnd.wqd,"
//                       + "application/vnd.wt.stf,"
//                       + "application/vnd.xara,"
//                       + "application/vnd.xfdl,"
//                       + "application/vnd.yamaha.hv-dic,"
//                       + "application/vnd.yamaha.hv-script,"
//                       + "application/vnd.yamaha.hv-voice,"
//                       + "application/vnd.yamaha.openscoreformat,"
//                       + "application/vnd.yamaha.openscoreformat.osfpvg+xml,"
//                       + "application/vnd.yamaha.smaf-audio,"
//                       + "application/vnd.yamaha.smaf-phrase,"
//                       + "application/vnd.yellowriver-custom-menu,"
//                       + "application/vnd.zul,"
//                       + "application/vnd.zzazz.deck+xml,"
//                       + "application/voicexml+xml,"
//                       + "application/widget,"
//                       + "application/winhlp,"
//                       + "application/wsdl+xml,"
//                       + "application/wspolicy+xml,"
//                       + "application/x-7z-compressed,"
//                       + "application/x-abiword,"
//                       + "application/x-ace-compressed,"
//                       + "application/x-authorware-bin,"
//                       + "application/x-authorware-map,"
//                       + "application/x-authorware-seg,"
//                       + "application/x-bcpio,"
//                       + "application/x-bittorrent,"
//                       + "application/x-bzip,"
//                       + "application/x-bzip2,"
//                       + "application/x-cdlink,"
//                       + "application/x-chat,"
//                       + "application/x-chess-pgn,"
//                       + "application/x-cpio,"
//                       + "application/x-csh,"
//                       + "application/x-debian-package,"
//                       + "application/x-director,"
//                       + "application/x-doom,"
//                       + "application/x-dtbncx+xml,"
//                       + "application/x-dtbook+xml,"
//                       + "application/x-dtbresource+xml,"
//                       + "application/x-dvi,"
//                       + "application/x-font-bdf,"
//                       + "application/x-font-ghostscript,"
//                       + "application/x-font-linux-psf,"
//                       + "application/x-font-otf,"
//                       + "application/x-font-pcf,"
//                       + "application/x-font-snf,"
//                       + "application/x-font-ttf,"
//                       + "application/x-font-type1,"
//                       + "application/x-font-woff,"
//                       + "application/x-futuresplash,"
//                       + "application/x-gnumeric,"
//                       + "application/x-gtar,"
//                       + "application/x-hdf,"
//                       + "application/x-java-jnlp-file,"
//                       + "application/x-latex,"
//                       + "application/x-mobipocket-ebook,"
//                       + "application/x-ms-application,"
//                       + "application/x-ms-wmd,"
//                       + "application/x-ms-wmz,"
//                       + "application/x-ms-xbap,"
//                       + "application/x-msaccess,"
//                       + "application/x-msbinder,"
//                       + "application/x-mscardfile,"
//                       + "application/x-msclip,"
//                       + "application/x-msdownload,"
//                       + "application/x-msmediaview,"
//                       + "application/x-msmetafile,"
//                       + "application/x-msmoney,"
//                       + "application/x-mspublisher,"
//                       + "application/x-msschedule,"
//                       + "application/x-msterminal,"
//                       + "application/x-mswrite,"
//                       + "application/x-netcdf,"
//                       + "application/x-pkcs12,"
//                       + "application/x-pkcs7-certificates,"
//                       + "application/x-pkcs7-certreqresp,"
//                       + "application/x-rar-compressed,"
//                       + "application/x-sh,"
//                       + "application/x-shar,"
//                       + "application/x-shockwave-flash,"
//                       + "application/x-silverlight-app,"
//                       + "application/x-stuffit,"
//                       + "application/x-stuffitx,"
//                       + "application/x-sv4cpio,"
//                       + "application/x-sv4crc,"
//                       + "application/x-tar,"
//                       + "application/x-tcl,"
//                       + "application/x-tex,"
//                       + "application/x-tex-tfm,"
//                       + "application/x-texinfo,"
//                       + "application/x-ustar,"
//                       + "application/x-wais-source,"
//                       + "application/x-x509-ca-cert,"
//                       + "application/x-xfig,"
//                       + "application/x-xpinstall,"
//                       + "application/xcap-diff+xml,"
//                       + "application/xenc+xml,"
//                       + "application/xhtml+xml,"
//                       + "application/xml,"
//                       + "application/xml-dtd,"
//                       + "application/xop+xml,"
//                       + "application/xslt+xml,"
//                       + "application/xspf+xml,"
//                       + "application/xv+xml,"
//                       + "application/yang,"
//                       + "application/yin+xml,"
//                       + "application/zip,"
//                       + "audio/adpcm,"
//                       + "audio/basic,"
//                       + "audio/midi,"
//                       + "audio/mp4,"
//                       + "audio/mpeg,"
//                       + "audio/ogg,"
//                       + "audio/vnd.dece.audio,"
//                       + "audio/vnd.digital-winds,"
//                       + "audio/vnd.dra,"
//                       + "audio/vnd.dts,"
//                       + "audio/vnd.dts.hd,"
//                       + "audio/vnd.lucent.voice,"
//                       + "audio/vnd.ms-playready.media.pya,"
//                       + "audio/vnd.nuera.ecelp4800,"
//                       + "audio/vnd.nuera.ecelp7470,"
//                       + "audio/vnd.nuera.ecelp9600,"
//                       + "audio/vnd.rip,"
//                       + "audio/webm,"
//                       + "audio/x-aac,"
//                       + "audio/x-aiff,"
//                       + "audio/x-mpegurl,"
//                       + "audio/x-ms-wax,"
//                       + "audio/x-ms-wma,"
//                       + "audio/x-pn-realaudio,"
//                       + "audio/x-pn-realaudio-plugin,"
//                       + "audio/x-wav,"
//                       + "chemical/x-cdx,"
//                       + "chemical/x-cif,"
//                       + "chemical/x-cmdf,"
//                       + "chemical/x-cml,"
//                       + "chemical/x-csml,"
//                       + "chemical/x-xyz,"
//                       + "image/bmp,"
//                       + "image/cgm,"
//                       + "image/g3fax,"
//                       + "image/gif,"
//                       + "image/ief,"
//                       + "image/jpeg,"
//                       + "image/ktx,"
//                       + "image/png,"
//                       + "image/prs.btif,"
//                       + "image/svg+xml,"
//                       + "image/tiff,"
//                       + "image/vnd.adobe.photoshop,"
//                       + "image/vnd.dece.graphic,"
//                       + "image/vnd.dvb.subtitle,"
//                       + "image/vnd.djvu,"
//                       + "image/vnd.dwg,"
//                       + "image/vnd.dxf,"
//                       + "image/vnd.fastbidsheet,"
//                       + "image/vnd.fpx,"
//                       + "image/vnd.fst,"
//                       + "image/vnd.fujixerox.edmics-mmr,"
//                       + "image/vnd.fujixerox.edmics-rlc,"
//                       + "image/vnd.ms-modi,"
//                       + "image/vnd.net-fpx,"
//                       + "image/vnd.wap.wbmp,"
//                       + "image/vnd.xiff,"
//                       + "image/webp,"
//                       + "image/x-cmu-raster,"
//                       + "image/x-cmx,"
//                       + "image/x-freehand,"
//                       + "image/x-icon,"
//                       + "image/x-pcx,"
//                       + "image/x-pict,"
//                       + "image/x-portable-anymap,"
//                       + "image/x-portable-bitmap,"
//                       + "image/x-portable-graymap,"
//                       + "image/x-portable-pixmap,"
//                       + "image/x-rgb,"
//                       + "image/x-xbitmap,"
//                       + "image/x-xpixmap,"
//                       + "image/x-xwindowdump,"
//                       + "message/rfc822,"
//                       + "model/iges,"
//                       + "model/mesh,"
//                       + "model/vnd.collada+xml,"
//                       + "model/vnd.dwf,"
//                       + "model/vnd.gdl,"
//                       + "model/vnd.gtw,"
//                       + "model/vnd.mts,"
//                       + "model/vnd.vtu,"
//                       + "model/vrml,"
//                       + "text/calendar,"
//                       + "text/css,"
//                       + "text/csv,"
//                       + "text/html,"
//                       + "text/n3,"
//                       + "text/plain,"
//                       + "text/prs.lines.tag,"
//                       + "text/richtext,"
//                       + "text/sgml,"
//                       + "text/tab-separated-values,"
//                       + "text/troff,"
//                       + "text/turtle,"
//                       + "text/uri-list,"
//                       + "text/vnd.curl,"
//                       + "text/vnd.curl.dcurl,"
//                       + "text/vnd.curl.scurl,"
//                       + "text/vnd.curl.mcurl,"
//                       + "text/vnd.fly,"
//                       + "text/vnd.fmi.flexstor,"
//                       + "text/vnd.graphviz,"
//                       + "text/vnd.in3d.3dml,"
//                       + "text/vnd.in3d.spot,"
//                       + "text/vnd.sun.j2me.app-descriptor,"
//                       + "text/vnd.wap.wml,"
//                       + "text/vnd.wap.wmlscript,"
//                       + "text/x-asm,"
//                       + "text/x-c,"
//                       + "text/x-fortran,"
//                       + "text/x-pascal,"
//                       + "text/x-java-source,java,"
//                       + "text/x-setext,"
//                       + "text/x-uuencode,"
//                       + "text/x-vcalendar,"
//                       + "text/x-vcard,"
//                       + "video/3gpp,"
//                       + "video/3gpp2,"
//                       + "video/h261,"
//                       + "video/h263,"
//                       + "video/h264,"
//                       + "video/jpeg,"
//                       + "video/jpm,"
//                       + "video/mj2,"
//                       + "video/mp4,"
//                       + "video/mpeg,"
//                       + "video/ogg,"
//                       + "video/quicktime,"
//                       + "video/vnd.dece.hd,"
//                       + "video/vnd.dece.mobile,"
//                       + "video/vnd.dece.pd,"
//                       + "video/vnd.dece.sd,"
//                       + "video/vnd.dece.video,"
//                       + "video/vnd.fvt,"
//                       + "video/vnd.mpegurl,"
//                       + "video/vnd.ms-playready.media.pyv,"
//                       + "video/vnd.uvvu.mp4,"
//                       + "video/vnd.vivo,"
//                       + "video/webm,"
//                       + "video/x-f4v,"
//                       + "video/x-fli,"
//                       + "video/x-flv,"
//                       + "video/x-m4v,"
//                       + "video/x-ms-asf,"
//                       + "video/x-ms-wm,"
//                       + "video/x-ms-wmv,"
//                       + "video/x-ms-wmx,"
//                       + "video/x-ms-wvx,"
//                       + "video/x-msvideo,"
//                       + "video/x-sgi-movie,"
//                       + "x-conference/x-cooltalk,"
//                       + "text/plain-bas,"
//                       + "text/yaml";
//        String[] typesArray = types.split(",");
//
//        weka.core.FastVector mimeTypes = new weka.core.FastVector(typesArray.length);
//        for (String mt : typesArray) {
//            mimeTypes.addElement(mt);
//        }
//        conentTypeAttribute = new weka.core.Attribute("conentType", mimeTypes);
//
//
//
//        locationPreferenceAttribute = new weka.core.Attribute("locationPreference");
//        descriptionAttribute = new weka.core.Attribute("description");
//
//
//
//
//        weka.core.FastVector lockDepths = new weka.core.FastVector(LockInfo.LockDepth.values().length);
//        for (LockDepth ld : LockInfo.LockDepth.values()) {
//            lockDepths.addElement(ld.toString());
//        }
//        lockDepthAttribute = new weka.core.Attribute("lockDepth", lockDepths);
//
//
//        weka.core.FastVector lockScopes = new weka.core.FastVector(LockInfo.LockDepth.values().length);
//        for (LockInfo.LockScope ls : LockInfo.LockScope.values()) {
//            lockScopes.addElement(ls.toString());
//        }
//        lockScopeAttribute = new weka.core.Attribute("lockScope", lockScopes);
//
//
//        lockTokenIDAttribute = new weka.core.Attribute("lockTokenID");
//
//
//
//
//        weka.core.FastVector lockTypes = new weka.core.FastVector(LockInfo.LockType.values().length);
//        for (LockInfo.LockType lt : LockInfo.LockType.values()) {
//            lockTypes.addElement(lt.toString());
//        }
//        lockTypeAttribute = new weka.core.Attribute("lockType", lockTypes);
//
//        lockedByUserAttribute = new weka.core.Attribute("lockedByUser");
//
//        nameAttribute = new weka.core.Attribute("name");
//
//
//        ownerAttribute = new weka.core.Attribute("owner");
//
//        statusAttribute = new weka.core.Attribute("status");
//        lockTimeoutAttribute = new weka.core.Attribute("lockTimeout");
//
//        weka.core.FastVector isSupervisedTypes = new weka.core.FastVector(2);
//        isSupervisedTypes.addElement("true");
//        isSupervisedTypes.addElement("false");
//        isSupervisedAttribute = new weka.core.Attribute("isSupervised", isSupervisedTypes);
//
//        weka.core.FastVector typeTypes = new weka.core.FastVector(3);
//        typeTypes.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_DATA);
//        typeTypes.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FILE);
//        typeTypes.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FOLDER);
//        typeAttribute = new weka.core.Attribute("type", typeTypes);
//
//        creationDateAttribute = new weka.core.Attribute("creationDate");
//        lastValidationDateAttribute = new weka.core.Attribute("lastValidationDate");
//
//        lengthAttribute = new weka.core.Attribute("length");
//
//
//
//        modifiedDateAttribute = new weka.core.Attribute("modifiedDate");
//
//        parentRefAttribute = new weka.core.Attribute("parentRef");
//
//        pdriGroupIdAttribute = new weka.core.Attribute("pdriGroupId");
//        uidAttribute = new weka.core.Attribute("uid");
    }

    private void sample() throws Exception {
        Sampling s = Sampling.SubSampling;

        //http://www.foxgo.net/uploads/2/1/3/8/2138775/jml-manual.pdf
        //The methods return a pair of data sets. The first part is the actual 
        //sample, the second part of the pair is a data set containing the 
        //out-of-bag samples
//        for (int i = 0; i < 5; i++) {
        Pair<Dataset, Dataset> datas = s.sample(fileDataset, (int) (fileDataset.size() * 0.8));
//            Pair<Dataset, Dataset> datas = s.sample(fileDataset, (int) (fileDataset.size() * 0.8), 1);
        fileDataset = datas.x();
        System.out.println("x_size: " + datas.x().size() + " y_size: " + datas.y().size() + " fileDataset_size: " + fileDataset.size());
        Classifier c = new LibSVM();
        c.buildClassifier(datas.x());
        //http://java-ml.sourceforge.net/api/0.1.3/net/sf/javaml/classification/evaluation/PerformanceMeasure.html
        Map pm = EvaluateDataset.testDataset(c, datas.y());
        System.out.println(pm);
        for (Iterator it = pm.keySet().iterator(); it.hasNext();) {
            String instanceName = (String) it.next();
            net.sf.javaml.classification.evaluation.PerformanceMeasure m = (net.sf.javaml.classification.evaluation.PerformanceMeasure) pm.get(instanceName);
            System.out.println(instanceName + " : " + m.getAccuracy());
        }
//        }
    }

    private void evaluateCluster() {
//        ClusterEvaluation sse = new AICScore();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.BICScore();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.HybridCentroidSimilarity();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.HybridPairwiseSimilarities();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.SumOfAveragePairwiseSimilarities();
        ClusterEvaluation sse = new net.sf.javaml.clustering.evaluation.SumOfCentroidSimilarities();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.SumOfSquaredErrors();
//        ClusterEvaluation sse =new net.sf.javaml.clustering.evaluation.TraceScatterMatrix();

        /* Measure the quality of the clustering */
        double score = sse.score(fileClusters);
        log.log(Level.INFO, "Cluster score: {0}", score);
    }
}

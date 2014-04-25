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
import io.milton.http.Request;
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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
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
import net.sf.javaml.distance.DistanceMeasure;
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
import weka.core.EuclideanDistance;
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
//    private Connection connection;
    private DataSource datasource;

    public LDClustering() throws NamingException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {

//        String jndiName = "jdbc/lobcder";
//        Context ctx = new InitialContext();
//        Context envContext = (Context) ctx.lookup("java:/comp/env");
//        datasource = (DataSource) envContext.lookup(jndiName);


        fileDataset = new DefaultDataset();
//        initAttributes();
    }

    public Connection getConnection() throws SQLException {
        Connection cn = datasource.getConnection();
        cn.setAutoCommit(false);
        return cn;
    }

    public static void main(String args[]) throws Exception {


//        DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
//        
//        datasource2 = DriverManager.getConnection(url);
//        datasource2.setAutoCommit(false);

        LDClustering c = new LDClustering();
 
        c.run();

        c.getNextState(new LobState(Request.Method.GET, "/DSS2IR/DSS2IR_area.fits"));
    }

    @Override
    public void run() {
        try {
            buildOrUpdateDataset();
//            printDataset();
//            featureScoring();
//            sample();
            normalizeDataset();
            printDataset();
//            featureScoring();
            cluster();
            printClusters();
//            evaluateCluster();
        } catch (SQLException ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void buildOrUpdateDataset() throws SQLException {
        try (Connection connection = getConnection()) {
            LogicalData root = getLogicalDataByUid(Long.valueOf(1), connection);
            addFeatures(Path.root, root, connection);
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

    private void addFeatures(Path p, LogicalData node, Connection connection) throws SQLException {

        ArrayList instances = getInstances(p, node, null);

        fileDataset.addAll(instances);


        Collection<LogicalData> children = getChildrenByParentRef(node.getUid(), connection);
        for (LogicalData n : children) {
            if (n.getUid() != node.getUid()) {
//                log.log(Level.INFO, "children: " + ld.getName());
                Path nextPath = Path.path(p, n.getName());
//                log.log(Level.INFO, "node: " + nextPath);
                addFeatures(nextPath, n, connection);
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
//            Dataset ds = fileClusters[i];
//
//            Iterator<Instance> iter = ds.iterator();
//
//            while (iter.hasNext()) {
//                Instance inst = iter.next();
//                String ids = i + " ------------------\nID:" + inst.getID() + " " + inst.classValue() + "\n------------------";
//                log.log(Level.INFO, ids);
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


        Instance rgB = fileDataset.get(0);
//        int id = rgB.getID();
//        System.out.println("------------------ ");
//        System.out.println("ID: " + id);
//        Object classValue = rgB.classValue();
//        if (classValue != null) {
//            System.out.println("classValue: " + classValue.getClass().getName());
//            System.out.println("classValue: " + classValue);
//        }
//        SortedSet<Integer> keys = rgB.keySet();
//        Iterator<Integer> kIter = keys.iterator();
//        while (kIter.hasNext()) {
//            Integer key = kIter.next();
//            Double val = rgB.get(key);
//            System.out.println("key: " + key + " val: " + val);
//        }
//        System.out.println("------------------ ");

        NormalizeMean nmr = new NormalizeMean();
        nmr.build(fileDataset);
        nmr.filter(fileDataset);
//        InstanceNormalizeMidrange inm = new InstanceNormalizeMidrange(-9999, 9999);
//        inm.build(fileDataset);
//        inm.filter(fileDataset);


//        rgB = fileDataset.get(0);
//        id = rgB.getID();
//        System.out.println("------------------ ");
//        System.out.println("ID: " + id);
//        classValue = rgB.classValue();
//        if (classValue != null) {
//            System.out.println("classValue: " + classValue.getClass().getName());
//            System.out.println("classValue: " + classValue);
//        }
//        keys = rgB.keySet();
//        kIter = keys.iterator();
//        while (kIter.hasNext()) {
//            Integer key = kIter.next();
//            Double val = rgB.get(key);
//            System.out.println("key: " + key + " val: " + val);
//        }
//        System.out.println("------------------ ");
    }

    private void sample() throws Exception {
        Sampling s = Sampling.SubSampling;

        //http://www.foxgo.net/uploads/2/1/3/8/2138775/jml-manual.pdf
        //The methods return a pair of data sets. The first part is the actual 
        //sample, the second part of the pair is a data set containing the 
        //out-of-bag samples
        log.log(Level.INFO, "file set size: " + fileDataset.size());
        Pair<Dataset, Dataset> datas = s.sample(fileDataset, (int) (fileDataset.size() * 0.8));
        fileDataset = datas.x();
        log.log(Level.INFO, "file set size: " + fileDataset.size());
//        Classifier c = new LibSVM();
//        c.buildClassifier(datas.x());
        //http://java-ml.sourceforge.net/api/0.1.3/net/sf/javaml/classification/evaluation/PerformanceMeasure.html
//        Map pm = EvaluateDataset.testDataset(c, datas.y());
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

    public LobState getNextState(LobState currentState) throws SQLException {
        String rName = currentState.getResourceName();
        LogicalData data = getLogicalDataByPath(Path.path(rName), this.getConnection());
        Instance instance = getInstances(Path.path(rName), data, currentState.getMethod()).get(0);
        DistanceMeasure dm = new net.sf.javaml.distance.EuclideanDistance();
        Set<Instance> res = fileDataset.kNearest(5, instance, dm);
        for (Instance i : fileDataset) {
            double dist = dm.measure(instance, i);
            log.log(Level.INFO, "EuclideanDistance" + instance.classValue() + "->" + i.classValue() + "\t\t=" + dist);
        }

//        dm = new net.sf.javaml.distance.AngularDistance();
//        for (Instance i : fileDataset) {
//            double dist = dm.measure(instance, i);
//            log.log(Level.INFO, "AngularDistance" + instance.classValue() + "->" + i.classValue() + "\t\t=" + dist);
//        }

        dm = new net.sf.javaml.distance.ChebychevDistance();
        for (Instance i : fileDataset) {
            double dist = dm.measure(instance, i);
            log.log(Level.INFO, "ChebychevDistance" + instance.classValue() + "->" + i.classValue() + "\t\t=" + dist);
        }

        dm = new net.sf.javaml.distance.ConsistencyIndex(3);
        for (Instance i : fileDataset) {
            double dist = dm.measure(instance, i);
            log.log(Level.INFO, "ConsistencyIndex" + instance.classValue() + "->" + i.classValue() + "\t\t=" + dist);
        }

        dm = new net.sf.javaml.distance.CosineDistance();
        for (Instance i : fileDataset) {
            double dist = dm.measure(instance, i);
            log.log(Level.INFO, "CosineDistance" + instance.classValue() + "->" + i.classValue() + "\t\t=" + dist);
        }

        for (Instance i : res) {
            log.log(Level.INFO, "kNearest: {0} {1}", new Object[]{i.getID(), i.classValue()});
            String[] stateID = ((String) i.classValue()).split(",");
            if (stateID.length == 1) {
//                return new LobState(Request.Method.valueOf(stateID[0]), "/");
            } else {
//                return new LobState(Request.Method.valueOf(stateID[0]), stateID[1]);
            }

        }


//                  instance = new DenseInstance(featuresArray, n.getName());
        return null;
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
                if (i == (parts.length - 1)) {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(
                            "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                            + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                            + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                            + "description, locationPreference, status "
                            + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            res = new LogicalData();
                            res.setUid(rs.getLong(1));
                            res.setParentRef(parent);
                            res.setOwner(rs.getString(2));
                            res.setType(rs.getString(3));
                            res.setName(p);
                            res.setCreateDate(rs.getTimestamp(4).getTime());
                            res.setModifiedDate(rs.getTimestamp(5).getTime());
                            res.setLength(rs.getLong(6));
                            res.setContentTypesAsString(rs.getString(7));
                            res.setPdriGroupId(rs.getLong(8));
                            res.setSupervised(rs.getBoolean(9));
                            res.setChecksum(rs.getString(10));
                            res.setLastValidationDate(rs.getLong(11));
                            res.setLockTokenID(rs.getString(12));
                            res.setLockScope(rs.getString(13));
                            res.setLockType(rs.getString(14));
                            res.setLockedByUser(rs.getString(15));
                            res.setLockDepth(rs.getString(16));
                            res.setLockTimeout(rs.getLong(17));
                            res.setDescription(rs.getString(18));
                            res.setDataLocationPreference(rs.getString(19));
                            res.setStatus(rs.getString(20));
                            return res;
                        } else {
                            return null;
                        }
                    }
                } else {
                    preparedStatement.setLong(1, parent);
                    preparedStatement.setString(2, p);
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        parent = rs.getLong(1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    private ArrayList<Instance> getInstances(Path p, LogicalData n, Request.Method method) {

        ArrayList<Double> featuresList = new ArrayList<>();

        Long cDate = n.getCreateDate();
        Double featureCDate = Double.valueOf(cDate);
        featuresList.add(featureCDate);

        Long vDate = n.getLastValidationDate();
        if (vDate == null) {
            vDate = Long.valueOf(0);
        }
//        featuresList.add(Double.valueOf(vDate));

        Long len = n.getLength();
        if (len == null) {
            len = Long.valueOf(0);
        }
        featuresList.add(Double.valueOf(len));


        Long lTimeout = n.getLockTimeout();
        if (lTimeout == null) {
            lTimeout = Long.valueOf(0);
        }
//        featuresList.add(Double.valueOf(lTimeout));


        Long mDate = n.getModifiedDate();
        if (mDate == null) {
            mDate = Long.valueOf(0);
        }
        featuresList.add(Double.valueOf(mDate));

        featuresList.add(Double.valueOf(n.getParentRef()));


//        featuresList.add(Double.valueOf(n.getPdriGroupId()));


//        featuresList.add(Double.valueOf(n.getUid()));


        String feature = n.getContentTypesAsString();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getDataLocationPreference();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getDescription();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getLockDepth();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getLockScope();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getLockTokenID();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getLockType();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        feature = n.getLockedByUser();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        String name = n.getName();
        if (name.length() <= 0) {
            name = "/";
        }
//        featuresList.add(toAscii(name));


        String owner = n.getOwner();
//        featuresList.add(toAscii(owner));

        feature = n.getStatus();
        if (feature == null || feature.length() <= 0) {
            feature = "NON";
        }
//        featuresList.add(toAscii(feature));

        Boolean isSupervised = n.getSupervised();
//        if (isSupervised) {
//            featuresList.add(1.0);
//        } else {
//            featuresList.add(0.0);
//        }

        String type = n.getType();
//        featuresList.add(toAscii(type));

        ArrayList<Instance> instances = new ArrayList<>();
        if (method == null) {
            for (Request.Method m : Request.Method.values()) {

                featuresList.add(getMethodFeature(m));

                double[] featuresArray = new double[featuresList.size()];
                for (int i = 0; i < featuresArray.length; i++) {
                    featuresArray[i] = featuresList.get(i);
                }
                instances.add(new DenseInstance(featuresArray, m + "," + p));
            }

        } else {
//            featuresList.add(toAscii(method.code));
            double[] featuresArray = new double[featuresList.size()];
            for (int i = 0; i < featuresArray.length; i++) {
                featuresArray[i] = featuresList.get(i);
            }
            instances.add(new DenseInstance(featuresArray, method + "," + p));
        }
        return instances;
    }

    private void printDataset() {
        Iterator<Instance> iter = fileDataset.iterator();

        while (iter.hasNext()) {
            Instance inst = iter.next();
            String ids = "------------------\nID:" + inst.getID() + " " + inst.classValue() + "\n------------------";
            SortedSet<Integer> key = inst.keySet();
            for (Integer in : key) {
                Double feature = inst.get(in);
                ids += "\n " + in + ":" + feature;
            }
            log.log(Level.INFO, ids);
        }
    }

    private void setDatasource(BasicDataSource datasource) {
        this.datasource = datasource;
    }

    private Double getMethodFeature(Request.Method m) {
        
        return Double.valueOf(0);
    }
}

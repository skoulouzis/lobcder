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

import io.milton.common.Path;
import io.milton.http.Request;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.core.Dataset;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.MyDataSource;
import org.apache.commons.dbcp.BasicDataSource;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.neighboursearch.BallTree;
import weka.core.neighboursearch.CoverTree;
import weka.core.neighboursearch.KDTree;
import weka.core.neighboursearch.NearestNeighbourSearch;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClusteringOld extends MyDataSource implements Runnable {

//    private Connection connection;
//    private DataSource datasource;
    private Instances dataset;
    private FastVector metdataAttributes;
    private String ROOT = "/lobcder/dav";

    public LDClusteringOld() throws NamingException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
//            String jndiName = "jdbc/lobcder";
//            Context ctx = new InitialContext();
//            Context envContext = (Context) ctx.lookup("java:/comp/env");
//            datasource = (DataSource) envContext.lookup(jndiName);



            initAttributes();


            // Create an empty training set
            dataset = new Instances("metadata", metdataAttributes, 0);
            dataset.setClassIndex(dataset.numAttributes() - 1);
            // Set class index
//        metadatSet.setClassIndex(3);

        } catch (ParseException ex) {
            Logger.getLogger(LDClusteringOld.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LDClusteringOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public Connection getConnection() throws SQLException {
//        Connection cn = datasource.getConnection();
//        cn.setAutoCommit(false);
//        return cn;
//    }
    public static void main(String args[]) throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        String dbName = "";
        String dbUname = "";
        String dbPasswd = "";
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUsername(dbUname);
        dataSource.setPassword(dbPasswd);
        String url = "jdbc:mysql://localhost:3306/" + dbName;
        dataSource.setUrl(url);
        dataSource.setMaxActive(10);
        dataSource.setMaxIdle(5);
        dataSource.setInitialSize(5);
        dataSource.setValidationQuery("SELECT 1");

//        DriverManager.registerDriver((Driver) Class.forName("com.mysql.jdbc.Driver").newInstance());
//        
//        datasource2 = DriverManager.getConnection(url);
//        datasource2.setAutoCommit(false);

        LDClusteringOld c = new LDClusteringOld();
//        c.setDatasource(dataSource);
        c.run();

        c.getNextState(new Vertex(Request.Method.GET, "/DSS2IR/DSS2IR_area.fits"));
    }

    @Override
    public void run() {
        try {
            buildOrUpdateDataset();
//            printDataset();
//            sample();
            normalizeDataset();
//            featureScoring();
//            printDataset();
//            featureScoring();
//            cluster();
//            printClusters();
//            evaluateCluster();
        } catch (SQLException ex) {
            Logger.getLogger(LDClusteringOld.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LDClusteringOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void buildOrUpdateDataset() throws SQLException, Exception {
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
//                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    private void addFeatures(Path p, LogicalData node, Connection connection) throws SQLException, Exception {

        ArrayList<Instance> instances = getInstances(p, node, null);
        for (Instance i : instances) {
            dataset.add(i);
        }

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
//                element.setDataLocationPreference(rs.getString(20));
                res.add(element);
            }
            return res;
        }
    }

    private void cluster() throws Exception {

//        weka.clusterers.EM clusterer = new EM();

        weka.clusterers.FarthestFirst clusterer = new weka.clusterers.FarthestFirst();
//        weka.clusterers.FilteredClusterer clusterer = new FilteredClusterer();
//        weka.clusterers.HierarchicalClusterer clusterer = new HierarchicalClusterer();
//        weka.clusterers.MakeDensityBasedClusterer clusterer = new MakeDensityBasedClusterer();
//        weka.clusterers.sIB clusterer = new sIB();
        clusterer.buildClusterer(dataset);
        log.log(Level.INFO, clusterer.globalInfo());
        log.log(Level.INFO, clusterer.toString());
    }

    private static void printClusters() {
    }

    private void featureScoring() throws Exception {

//        ASEvaluation eval = new GainRatioAttributeEval();
//        ASEvaluation eval = new CfsSubsetEval();
//        ASEvaluation eval = new ChiSquaredAttributeEval();
//        ASEvaluation eval = new ClassifierSubsetEval();
//        ASEvaluation eval = new ConsistencySubsetEval();
//        ASEvaluation eval = new CostSensitiveAttributeEval();
//        ASEvaluation eval = new CostSensitiveSubsetEval();
//         ASEvaluation eval = new FilteredAttributeEval();
//        ASEvaluation eval = new FilteredSubsetEval();
//         ASEvaluation eval = new GainRatioAttributeEval();
//        ASEvaluation eval = new InfoGainAttributeEval();
//        ASEvaluation eval = new LatentSemanticAnalysis();
//        ASEvaluation eval = new OneRAttributeEval();
//        ASEvaluation eval = new OneRAttributeEval();
        PrincipalComponents eval = new PrincipalComponents();

        eval.buildEvaluator(dataset);
//        log.log(Level.INFO, eval.toString());


        Map<Attribute, Double> infogainscores = new HashMap<>();
        for (int i = 0; i < dataset.numAttributes(); i++) {
            Attribute t_attr = dataset.attribute(i);
            double infogain = eval.evaluateAttribute(i);
            infogainscores.put(t_attr, infogain);
            log.log(Level.INFO, "{0} : {1}", new Object[]{t_attr, infogain});
        }

    }

    private void normalizeDataset() throws Exception {
        //preprocess strings (almost no classifier supports them)
        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(dataset);
        dataset = Filter.useFilter(dataset, filter);

        Normalize m_normalize = new Normalize();
        m_normalize.setInputFormat(dataset);
        dataset = Filter.useFilter(dataset, m_normalize);
    }

    private void sample() throws Exception {
        //split to 70:30 learn and test set
        double percent = 20.0;
        int trainSize = (int) Math.round(dataset.numInstances() * percent / 100);
        int testSize = dataset.numInstances() - trainSize;
        Instances train = new Instances(dataset, 0, trainSize);
        Instances test = new Instances(dataset, trainSize, testSize);
        train.setClassIndex(1);
        test.setClassIndex(1);
    }

    private void evaluateCluster() {
    }

    public Vertex getNextState(Vertex currentState) throws SQLException, Exception {
        String rName = currentState.getResourceName();
        if (rName.equals(ROOT)) {
            rName = "";
        }
        LogicalData data = getLogicalDataByPath(Path.path(rName), this.getConnection());
        Instance instance = getInstances(Path.path(rName), data, currentState.getMethod()).get(0);
        Instances dataUnlabeled = new Instances("TestInstances", metdataAttributes, 0);
        dataUnlabeled.add(instance);
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);

//        NormalizableDistance dist = new EuclideanDistance(dataset);
//        NormalizableDistance dist = new ChebyshevDistance(dataset);
//        NormalizableDistance dist = new EditDistance(dataset);
//        NormalizableDistance dist = new ManhattanDistance(dataset);
//        for (int i = 0; i < dataset.numInstances(); i++) {
//            Instance di = dataset.instance(i);
//            double res = dist.distance(instance, di);
//            System.err.println(di + dist.getClass().getSimpleName() + " : " + res);
//        }
//        NearestNeighbourSearch nns = new BallTree(dataset);
        NearestNeighbourSearch nns = new KDTree(dataset);
        nns.setInstances(dataset);
        Instances res = nns.kNearestNeighbours(dataUnlabeled.firstInstance(), 10);
        Enumeration<Instance> en = res.enumerateInstances();
        while (en.hasMoreElements()) {
            Instance in = en.nextElement();
            log.log(Level.INFO, in.attribute(8).toString());
        }
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
//                            res.setDataLocationPreference(rs.getString(19));
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

        ArrayList<Instance> instances = new ArrayList<>();
        if (method == null) {
            for (Request.Method m : Request.Method.values()) {
                Instance instance = new Instance(metdataAttributes.size());
                String att = n.getChecksum();
                instance.setValue(null, att);
                instance.setValue((Attribute) metdataAttributes.elementAt(0), (att != null) ? att : "NON");
                att = n.getContentTypesAsString();
                instance.setValue((Attribute) metdataAttributes.elementAt(1), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(2), n.getCreateDate());
//                att = n.getDataLocationPreference();
                instance.setValue((Attribute) metdataAttributes.elementAt(3), (att != null) ? att : "NON");
                att = n.getDescription();
                instance.setValue((Attribute) metdataAttributes.elementAt(4), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(5), n.getLastValidationDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(6), n.getLength());
                instance.setValue((Attribute) metdataAttributes.elementAt(7), n.getModifiedDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(8), p.toString());
                instance.setValue((Attribute) metdataAttributes.elementAt(9), n.getParentRef());
                att = n.getStatus();
                instance.setValue((Attribute) metdataAttributes.elementAt(10), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(11), n.getType());

                instance.setValue((Attribute) metdataAttributes.elementAt(12), String.valueOf(n.getSupervised()));
                instance.setValue((Attribute) metdataAttributes.elementAt(13), n.getUid());
                instance.setValue((Attribute) metdataAttributes.elementAt(14), m.code);
                instance.setValue((Attribute) metdataAttributes.elementAt(15), n.getOwner());

                int i = 0;
                for (double d : instance.toDoubleArray()) {
                    i++;
                    log.log(Level.INFO, "Attribute[{0}]: {1}", new Object[]{i, d});
                }

//                instance.setID(m + "," + p);
                instances.add(instance);
            }
        } else {
            Instance instance = new Instance(metdataAttributes.size());

            String att = n.getChecksum();
            instance.setValue((Attribute) metdataAttributes.elementAt(0), (att != null) ? att : "NON");
            att = n.getContentTypesAsString();
            instance.setValue((Attribute) metdataAttributes.elementAt(1), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(2), n.getCreateDate());
//            att = n.getDataLocationPreference();
            instance.setValue((Attribute) metdataAttributes.elementAt(3), (att != null) ? att : "NON");
            att = n.getDescription();
            instance.setValue((Attribute) metdataAttributes.elementAt(4), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(5), n.getLastValidationDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(6), n.getLength());
            instance.setValue((Attribute) metdataAttributes.elementAt(7), n.getModifiedDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(8), p.toString());
            instance.setValue((Attribute) metdataAttributes.elementAt(9), n.getParentRef());
            att = n.getStatus();
            instance.setValue((Attribute) metdataAttributes.elementAt(10), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(11), n.getType());

            instance.setValue((Attribute) metdataAttributes.elementAt(12), String.valueOf(n.getSupervised()));
            instance.setValue((Attribute) metdataAttributes.elementAt(13), n.getUid());
            instance.setValue((Attribute) metdataAttributes.elementAt(14), method.code);
            instance.setValue((Attribute) metdataAttributes.elementAt(15), n.getOwner());
//            instance.setID(method + "," + p);
            instances.add(instance);
        }


        return instances;
    }

    private void printDataset() {
        log.log(Level.INFO, dataset.toSummaryString());
        log.log(Level.INFO, dataset.toString());
    }

//    private void setDatasource(BasicDataSource datasource) {
//        this.datasource = datasource;
//    }
    private Double getMethodFeature(Request.Method m) {

        return Double.valueOf(0);
    }

    private void initAttributes() throws ParseException, Exception {
        Attribute checksumAttribute = new Attribute("checksum", (FastVector) null);

        Attribute contentTypeAttribute = new Attribute("contentType", (FastVector) null);

        Attribute createDateAttribute = new Attribute("createDate", "yyyy-MM-dd HH:mm:ss");
        Attribute locationPreferenceAttribute = new Attribute("locationPreference", (FastVector) null);
        Attribute descriptionAttribute = new Attribute("description", (FastVector) null);
        Attribute validationDateAttribute = new Attribute("validationDate", "yyyy-MM-dd HH:mm:ss");

        Attribute lengthAttribute = new Attribute("length");
        Attribute modifiedDateAttribute = new Attribute("modifiedDate", "yyyy-MM-dd HH:mm:ss");
        Attribute pathAttribute = new Attribute("name", (FastVector) null);
        Attribute parentRefAttribute = new Attribute("parentRef");
        Attribute statusAttribute = new Attribute("status", (FastVector) null);
        FastVector typeVector = new FastVector(3);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_DATA);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FILE);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FOLDER);
        Attribute typeAttribute = new Attribute("type", typeVector);

        // Declare the class attribute along with its values
        FastVector supervisedVector = new FastVector(2);
        supervisedVector.addElement("true");
        supervisedVector.addElement("false");
        Attribute supervisedAttribute = new Attribute("supervised", supervisedVector);

        Attribute uidAttribute = new Attribute("uid");


        // Declare a nominal attribute along with its values
        FastVector verbVector = new FastVector(Request.Method.values().length);
        for (Request.Method m : Request.Method.values()) {
            verbVector.addElement(m.code);
        }
        Attribute verbAttribute = new Attribute("verb", verbVector);
        Attribute ownerAttribute = new Attribute("owner", (FastVector) null);


        // Declare the feature vector
        metdataAttributes = new FastVector();
        metdataAttributes.addElement(checksumAttribute);//0
        metdataAttributes.addElement(contentTypeAttribute);//1
        metdataAttributes.addElement(createDateAttribute);//2
        metdataAttributes.addElement(locationPreferenceAttribute);//3
        metdataAttributes.addElement(descriptionAttribute);//4
        metdataAttributes.addElement(validationDateAttribute);//5
        metdataAttributes.addElement(lengthAttribute);//6
        metdataAttributes.addElement(modifiedDateAttribute);//7
        metdataAttributes.addElement(pathAttribute);//8
        metdataAttributes.addElement(parentRefAttribute);//9
        metdataAttributes.addElement(statusAttribute);//10
        metdataAttributes.addElement(typeAttribute);//11
        metdataAttributes.addElement(supervisedAttribute);//12
        metdataAttributes.addElement(uidAttribute);//13
        metdataAttributes.addElement(verbAttribute);//14
        metdataAttributes.addElement(ownerAttribute);//15



//        double[] attValues = new double[dataset.numAttributes()];
////	attValues[0] = 55;
//        attValues[0] = dataset.attribute("verb").indexOfValue(Request.Method.PUT.code);
//        attValues[1] = dataset.attribute("isSupervised").indexOfValue(String.valueOf(true));
//        attValues[2] = dataset.attribute("dateTime").parseDate("2009-07-15 10:10");
//        attValues[3] = dataset.attribute("owner").addStringValue("alogo");
//        // add the instance
//        dataset.add(new Instance(1.0, attValues));





//        iExample = new Instance(4);
//        iExample.setValue(verbAttribute, Request.Method.PROPFIND.code);
//        iExample.setValue(supervisedAttribute, String.valueOf(false));
//        iExample.setValue(dateTimeAttribute, dateTimeAttribute.parseDate("2009-03-20 12:00"));
//        iExample.setValue(ownerAttribute, "alogo");
//        dataset.add(iExample);
//
//
//        System.out.println(dataset);
//
//

//        System.out.println(dataset);
//
//        //8.build classifier
//        dataset.setClassIndex(1);
//        Classifier classifier = new J48();
//        classifier.buildClassifier(dataset);
//
//
//
//        //11.evaluate
//        //resample if needed
////        dataset = dataset.resample(new Random(42));
//
//
//        //split to 70:30 learn and test set
//        double percent = 70.0;
//        int trainSize = (int) Math.round(dataset.numInstances() * percent / 100);
//        int testSize = dataset.numInstances() - trainSize;
//        Instances train = new Instances(dataset, 0, trainSize);
//        Instances test = new Instances(dataset, trainSize, testSize);
//        train.setClassIndex(1);
//        test.setClassIndex(1);
//
//        //do eval
//        Evaluation eval = new Evaluation(train); //trainset
//        eval.evaluateModel(classifier, test); //testset
//        System.out.println(eval.toSummaryString());
//        System.out.println(eval.weightedFMeasure());
//        System.out.println(eval.weightedPrecision());
//        System.out.println(eval.weightedRecall());
//
//
//
//        //12.classify
//        //result
//        System.out.println(classifier.classifyInstance(dataset.firstInstance()));
//        //classified result value
//        System.out.println(dataset.attribute(dataset.classIndex()).value((int) dataset.firstInstance().classValue()));
//        System.out.println(classifier.distributionForInstance(dataset.firstInstance()));
//        Instance iUse = new Instance(4);
//        iUse.setValue(verbAttribute, Request.Method.DELETE.code);
//        iUse.setValue(supervisedAttribute, String.valueOf(false));
//        iUse.setValue(dateTimeAttribute, dateTimeAttribute.parseDate("2013-03-20 12:00"));
//        iUse.setValue(ownerAttribute, "alogo");
//        iUse.setDataset(dataset);
//
//        // Get the likelihood of each classes 
//        // fDistribution[0] is the probability of being “positive” 
//        // fDistribution[1] is the probability of being “negative” 
//        double[] fDistribution = classifier.distributionForInstance(iUse);
//        for (double d : fDistribution) {
//            System.err.println("d: " + d);
//        }
//
//
//
//        iUse = new Instance(4);
//        iUse.setValue(verbAttribute, Request.Method.MKCOL.code);
//        iUse.setValue(supervisedAttribute, String.valueOf(false));
//        iUse.setValue(dateTimeAttribute, dateTimeAttribute.parseDate("2014-04-20 12:00"));
//        iUse.setValue(ownerAttribute, "alogo");
//        iUse.setDataset(dataset);
//
//        EuclideanDistance dist = new EuclideanDistance(dataset);
//        for (int i = 0; i < dataset.numInstances(); i++) {
//            Instance di = dataset.instance(i);
//            double res = dist.distance(iUse, di);
//            System.err.println(di + " EuclideanDistance: " + res);
//        }


    }
}

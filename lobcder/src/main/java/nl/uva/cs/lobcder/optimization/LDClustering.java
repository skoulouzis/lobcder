/*
 * Copyright 2014 S. Koulouzis.
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
import io.milton.http.Request.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.predictors.DBMapPredictor;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClustering extends DBMapPredictor implements Runnable {

    private FastVector metdataAttributes;
    private int k;

    public LDClustering() throws NamingException, ParseException, Exception {
        initAttributes();
        k = PropertiesHelper.KNN();
//        buildOrUpdateDataset();
    }

    private void initAttributes() throws ParseException, Exception {
        int index = 0;
        Attribute uidAttribute = new Attribute("uid", index++);
        // Declare a nominal attribute along with its values
        FastVector verbVector = new FastVector(Request.Method.values().length);
        for (Request.Method m : Request.Method.values()) {
            verbVector.addElement(m.code);
        }

        Attribute verbAttribute = new Attribute("verb", verbVector, index++);
        Attribute checksumAttribute = new Attribute("checksum", (FastVector) null, index++);

        Attribute contentTypeAttribute = new Attribute("contentType", (FastVector) null, index++);

        Attribute createDateAttribute = new Attribute("createDate", "yyyy-MM-dd HH:mm:ss", index++);
        Attribute locationPreferenceAttribute = new Attribute("locationPreference", (FastVector) null, index++);
        Attribute descriptionAttribute = new Attribute("description", (FastVector) null, index++);
        Attribute validationDateAttribute = new Attribute("validationDate", "yyyy-MM-dd HH:mm:ss", index++);

        Attribute lengthAttribute = new Attribute("length", index++);
        Attribute modifiedDateAttribute = new Attribute("modifiedDate", "yyyy-MM-dd HH:mm:ss", index++);
        Attribute pathAttribute = new Attribute("name", (FastVector) null, index++);
        Attribute parentRefAttribute = new Attribute("parentRef", index++);
        Attribute statusAttribute = new Attribute("status", (FastVector) null, index++);
        FastVector typeVector = new FastVector(3);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_DATA);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FILE);
        typeVector.addElement(nl.uva.cs.lobcder.util.Constants.LOGICAL_FOLDER);
        Attribute typeAttribute = new Attribute("type", typeVector, index++);

        // Declare the class attribute along with its values
        FastVector supervisedVector = new FastVector(2);
        supervisedVector.addElement("true");
        supervisedVector.addElement("false");
        Attribute supervisedAttribute = new Attribute("supervised", supervisedVector, index++);
        Attribute ownerAttribute = new Attribute("owner", (FastVector) null, index++);


        // Declare the feature vector
        metdataAttributes = new FastVector();
        metdataAttributes.addElement(uidAttribute);//0
        metdataAttributes.addElement(verbAttribute);//1
        metdataAttributes.addElement(checksumAttribute);//2
        metdataAttributes.addElement(contentTypeAttribute);//3
        metdataAttributes.addElement(createDateAttribute);//4
        metdataAttributes.addElement(locationPreferenceAttribute);//5
        metdataAttributes.addElement(descriptionAttribute);//6
        metdataAttributes.addElement(validationDateAttribute);//7
        metdataAttributes.addElement(lengthAttribute);//8
        metdataAttributes.addElement(modifiedDateAttribute);//9
        metdataAttributes.addElement(pathAttribute);//10
        metdataAttributes.addElement(parentRefAttribute);//11
        metdataAttributes.addElement(statusAttribute);//12
        metdataAttributes.addElement(typeAttribute);//13
        metdataAttributes.addElement(supervisedAttribute);//14
        metdataAttributes.addElement(ownerAttribute);//15
    }

    private void buildOrUpdateDataset() throws SQLException, Exception {
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT uid, parentRef, ownerId, datatype, ldName, "
                    + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                    + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                    + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                    + "FROM ldata_table")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    LogicalData res = new LogicalData();
                    res.setUid(rs.getLong(1));
                    res.setParentRef(rs.getLong(2));
                    res.setOwner(rs.getString(3));
                    res.setType(rs.getString(4));
                    res.setName(rs.getString(5));
                    res.setCreateDate(rs.getTimestamp(6).getTime());
                    res.setModifiedDate(rs.getTimestamp(7).getTime());
                    res.setLength(rs.getLong(8));
                    res.setContentTypesAsString(rs.getString(9));
                    res.setPdriGroupId(rs.getLong(10));
                    res.setSupervised(rs.getBoolean(11));
                    res.setChecksum(rs.getString(12));
                    res.setLastValidationDate(rs.getLong(13));
                    res.setLockTokenID(rs.getString(14));
                    res.setLockScope(rs.getString(15));
                    res.setLockType(rs.getString(16));
                    res.setLockedByUser(rs.getString(17));
                    res.setLockDepth(rs.getString(18));
                    res.setLockTimeout(rs.getLong(19));
                    res.setDescription(rs.getString(20));
                    res.setDataLocationPreference(rs.getString(21));
                    res.setStatus(rs.getString(22));
                    ArrayList<MyInstance> ins = getInstances(res, null);
                    for (MyInstance i : ins) {
                        addFeatures(connection, i, res.getUid());
                    }
                }
            }
        }
    }

    @Override
    public void run() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LobState getNextState(LobState currentState) {
        ArrayList<LobState> states = new ArrayList<>();
        String rName = currentState.getResourceName();
        if (!rName.endsWith("/")) {
            rName += "/";
        }
        rName = rName.replaceFirst("/lobcder/dav/", "");
        try (Connection connection = getConnection()) {
            LogicalData data = getLogicalDataByPath(Path.path(rName), connection);
            Instance instance = getInstances(data, currentState.getMethod()).get(0);
            double[] features = instance.toDoubleArray();
            String queryAllFeatures = "SELECT ldataRef, methodName, "
                    + "POW((f1 - ?), 2) + POW((f2 - ?), 2) + POW((f3 - ?), 2) + "
                    + "POW((f4 - ?), 2) + POW((f5 - ?), 2) + POW((f6 - ?), 2) + "
                    + "POW((f7 - ?), 2)+ POW((f8 - ?), 2)+ POW((f9 - ?), 2)+ "
                    + "POW((f10 - ?), 2)+ POW((f11 - ?), 2)+ POW((f12 - ?), 2)+ "
                    + "POW((f13 - ?), 2)+ POW((f14 - ?), 2)+ POW((f15 - ?), 2)+ "
                    + "POW((f16 - ?), 2)"
                    + "AS dist FROM features_table  ORDER BY dist ASC LIMIT ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    queryAllFeatures)) {
                int index = 1;
                for (int i = 1; i < features.length; i++) {
                    preparedStatement.setDouble(index++, features[i]);
                }

                preparedStatement.setInt(features.length, k);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    String path = getPathforLogicalData(getLogicalDataByUid(rs.getLong(1), connection), connection);
                    LobState state = new LobState(Method.valueOf(rs.getString(2)), path);
                    log.log(Level.INFO, "State: {0}", state.getID());
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(LDClustering.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void addFeatures(Connection connection, MyInstance inst, Long uid) throws SQLException {
        boolean exists = false;
        try (PreparedStatement ps = connection.prepareStatement("select uid "
                + "from features_table WHERE methodName = ? AND ldataRef = ?")) {
            ps.setString(1, inst.getMethod().code);
            ps.setLong(2, uid);
            ResultSet rs = ps.executeQuery();
            exists = rs.next();
        }
        if (!exists) {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                    + "features_table (methodName, ldataRef, f1, f2, f3, f4, f5, "
                    + "f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) "
                    + "VALUES (?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, inst.getMethod().name());
                ps.setLong(2, uid);
                double[] features = inst.toDoubleArray();
                for (int i = 0; i < features.length; i++) {
                    int index = i + 3;
                    ps.setDouble(index, features[i]);
                }
                ps.executeUpdate();
                connection.commit();
            }
        }
    }

    private ArrayList<MyInstance> getInstances(LogicalData n, Method method) {
        ArrayList<MyInstance> inst = new ArrayList<>();

        if (method == null) {
            for (Request.Method m : Request.Method.values()) {
                int index = 0;
                MyInstance instance = new MyInstance(metdataAttributes.size());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getUid());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), m.code);
                instance.setMethod(m);
                String att = n.getChecksum();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                att = n.getContentTypesAsString();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getCreateDate());
                att = n.getDataLocationPreference();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                att = n.getDescription();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLastValidationDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLength());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getModifiedDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getParentRef());
                att = n.getStatus();
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getType());

                instance.setValue((Attribute) metdataAttributes.elementAt(index++), String.valueOf(n.getSupervised()));
                instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getOwner());
                inst.add(instance);
            }
        } else {
            int index = 0;
            MyInstance instance = new MyInstance(metdataAttributes.size());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getUid());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), method.code);
            instance.setMethod(method);
            String att = n.getChecksum();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            att = n.getContentTypesAsString();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getCreateDate());
            att = n.getDataLocationPreference();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            att = n.getDescription();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLastValidationDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getLength());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getModifiedDate());
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getParentRef());
            att = n.getStatus();
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), (att != null) ? att : "NON");
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getType());

            instance.setValue((Attribute) metdataAttributes.elementAt(index++), String.valueOf(n.getSupervised()));
            instance.setValue((Attribute) metdataAttributes.elementAt(index++), n.getOwner());
            inst.add(instance);
        }
        return inst;
    }

    private class MyInstance extends Instance {

        private Method method;

        private MyInstance(int size) {
            super(size);
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Method getMethod() {
            return this.method;
        }
    }
}

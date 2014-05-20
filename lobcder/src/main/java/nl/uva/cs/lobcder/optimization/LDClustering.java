/*
 * Copyright 2014 alogo.
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
import java.sql.Statement;
import java.text.ParseException;
import java.util.logging.Level;
import javax.naming.NamingException;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.MyDataSource;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class LDClustering extends MyDataSource implements Runnable {

    private FastVector metdataAttributes;

    public LDClustering() throws NamingException, ParseException, Exception {
        initAttributes();

        buildOrUpdateDataset();
    }

    private void initAttributes() throws ParseException, Exception {
        int index = 0;
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

        Attribute uidAttribute = new Attribute("uid", index++);


        // Declare a nominal attribute along with its values
        FastVector verbVector = new FastVector(Request.Method.values().length);
        for (Request.Method m : Request.Method.values()) {
            verbVector.addElement(m.code);
        }
        Attribute verbAttribute = new Attribute("verb", verbVector, index++);
        Attribute ownerAttribute = new Attribute("owner", (FastVector) null, index++);
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


        Attribute a = (Attribute) metdataAttributes.elementAt(0);
        index = a.index();

        a = (Attribute) metdataAttributes.elementAt(1);
        index = a.index();
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
                    Instance ins = getInstance(res, null);
                    addFeatures(connection, ins, res.getUid());
                }
            }
        }
    }

    @Override
    public void run() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public LobState getNextState(LobState currentState) {
        return null;
    }

    private void addFeatures(Connection connection, Instance rs, Long uid) throws SQLException {
        String query = "IINSERT INTO features_table "
                + "(uid, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + "SELECT * FROM (SELECT '" + uid + "') AS tmp WHERE NOT EXISTS "
                + "(SELECT keyVal FROM features_table WHERE keyVal = '" + uid + "') LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, uid);
            double[] features = rs.toDoubleArray();
            for (int i = 0; i < features.length; i++) {
                int index = i + 2;
                ps.setDouble(index, features[i]);
            }
            ps.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            if (ex.getMessage().contains("Duplicate column name")) {
                connection.rollback();
            }
        }
    }

    private Instance getInstance(LogicalData n, Method method) {
        Instance instance = null;
        if (method == null) {
            for (Request.Method m : Request.Method.values()) {
                instance = new Instance(metdataAttributes.size());
                String att = n.getChecksum();
                instance.setValue((Attribute) metdataAttributes.elementAt(0), (att != null) ? att : "NON");
                att = n.getContentTypesAsString();
                instance.setValue((Attribute) metdataAttributes.elementAt(1), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(2), n.getCreateDate());
                att = n.getDataLocationPreference();
                instance.setValue((Attribute) metdataAttributes.elementAt(3), (att != null) ? att : "NON");
                att = n.getDescription();
                instance.setValue((Attribute) metdataAttributes.elementAt(4), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(5), n.getLastValidationDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(6), n.getLength());
                instance.setValue((Attribute) metdataAttributes.elementAt(7), n.getModifiedDate());
                instance.setValue((Attribute) metdataAttributes.elementAt(8), "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(9), n.getParentRef());
                att = n.getStatus();
                instance.setValue((Attribute) metdataAttributes.elementAt(10), (att != null) ? att : "NON");
                instance.setValue((Attribute) metdataAttributes.elementAt(11), n.getType());

                instance.setValue((Attribute) metdataAttributes.elementAt(12), String.valueOf(n.getSupervised()));
                instance.setValue((Attribute) metdataAttributes.elementAt(13), n.getUid());
                instance.setValue((Attribute) metdataAttributes.elementAt(14), m.code);
                instance.setValue((Attribute) metdataAttributes.elementAt(15), n.getOwner());
            }
        }
        return instance;
    }
}

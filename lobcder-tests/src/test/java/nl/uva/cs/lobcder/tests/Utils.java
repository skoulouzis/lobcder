/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.Status;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author S. Koulouzis
 */
public class Utils {

    private HttpClient client;

    public Utils(HttpClient client) {
        this.client = client;
    }

    public void waitForReplication(String resource) throws IOException, DavException, InterruptedException {
        boolean done = false;
        int count = 0;
        String[] availStorageSites = getAvailableStorageSites(resource);
        while (!done) {
            DavPropertyName dataDistributionName = DavPropertyName.create("data-distribution", Namespace.getNamespace("custom:"));
            MultiStatus multiStatus = getProperty(resource, dataDistributionName, true);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            String dataDist = null;
            for (MultiStatusResponse r : responses) {
//                System.out.println("Response: " + r.getHref());
                DavPropertySet allProp = getProperties(r);
                DavPropertyIterator iter = allProp.iterator();

                while (iter.hasNext()) {
                    DavProperty<?> p = iter.nextProperty();
                    assertEquals(p.getName(), dataDistributionName);
                    assertNotNull(p.getValue());
                    dataDist = (String) p.getValue();
                    if (dataDist.contains(availStorageSites[0])) {
                        done = true;
                        assertTrue(dataDist.contains(availStorageSites[0]));
                        break;
                    } else if (availStorageSites.length > 1 && dataDist.contains(availStorageSites[1])) {
                        done = true;
                        assertTrue(dataDist.contains(availStorageSites[1]));
                        break;
                    } else {
                        count++;
                    }
//                    System.out.println(p.getName() + " : " + p.getValue());
                }
            }
            if (count > 200) {
                fail(resource + " is not replicated in " + availStorageSites[0] + " or " + availStorageSites[1] + " it is in " + dataDist);
                break;
            }
            if (done) {
                break;
            } else {
                Thread.sleep(7000);
            }
        }
    }

    public String[] getAvailableStorageSites(String testuri1) throws IOException, DavException {
        DavPropertyNameSet availableStorageSitesNameSet = new DavPropertyNameSet();
        DavPropertyName availableStorageSitesName = DavPropertyName.create("avail-storage-sites", Namespace.getNamespace("custom:"));
        availableStorageSitesNameSet.add(availableStorageSitesName);


        PropFindMethod propFind = new PropFindMethod(testuri1, availableStorageSitesNameSet, DavConstants.DEPTH_INFINITY);
        int status = client.executeMethod(propFind);
        assertEquals(HttpStatus.SC_MULTI_STATUS, status);

        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();

        String value = null;
        for (MultiStatusResponse r : responses) {
            DavPropertySet allProp = getProperties(r);
            DavPropertyIterator iter = allProp.iterator();
            while (iter.hasNext()) {
                DavProperty<?> p = iter.nextProperty();
                assertEquals(p.getName(), availableStorageSitesName);
                assertNotNull(p.getValue());
                value = (String) p.getValue();
            }
        }

        String sites = value.replaceAll("[\\[\\]]", "");
        return sites.split(",");
    }

    public MultiStatus getProperty(String resource, DavPropertyName propertyName, boolean mustExists) throws IOException, DavException {
        DavPropertyNameSet commentNameSet1 = new DavPropertyNameSet();
        commentNameSet1.add(propertyName);

        PropFindMethod propFind = new PropFindMethod(resource, commentNameSet1, DavConstants.DEPTH_INFINITY);
        int status = client.executeMethod(propFind);
        if (mustExists) {
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);
        }
        return propFind.getResponseBodyAsMultiStatus();
    }

    public DavPropertySet getProperties(MultiStatusResponse statusResponse) {
        Status[] status = statusResponse.getStatus();

        DavPropertySet allProp = new DavPropertySet();
        for (int i = 0; i < status.length; i++) {
            DavPropertySet pset = statusResponse.getProperties(status[i].getStatusCode());
            allProp.addAll(pset);
        }

        return allProp;
    }

    Long getResourceUID(String resource) throws IOException, DavException {
        DavPropertyName etag = DavPropertyName.create("getetag", Namespace.getNamespace("DAV:"));
        MultiStatus multiStatus = getProperty(resource, etag, true);
        MultiStatusResponse[] responses = multiStatus.getResponses();
        String value = null;
//        for (MultiStatusResponse r : responses) {
        DavPropertySet allProp = getProperties(responses[0]);
        DavPropertyIterator iter = allProp.iterator();
        while (iter.hasNext()) {
            DavProperty<?> p = iter.nextProperty();
            assertNotNull(p.getValue());
            assertEquals(p.getName(), etag);
            value = (String) p.getValue();
            value = value.replaceAll("\"", "");
            break;
        }
//        }

        return Long.valueOf(value.split("_")[0]);
    }

    public void createFile(String resource, boolean mustSucceed) throws UnsupportedEncodingException, IOException {
        PutMethod put = new PutMethod(resource);
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        int status = this.client.executeMethod(put);
        if (mustSucceed) {
            assertEquals(HttpStatus.SC_CREATED, status);
        }
    }

    public void createCollection(String resource, boolean mustSucceed) throws IOException {
        MkColMethod mkcol = new MkColMethod(resource);
        int status = this.client.executeMethod(mkcol);
        if (mustSucceed) {
            assertEquals(HttpStatus.SC_CREATED, status);
        }

//        PutMethod put = new PutMethod(this.root + testResourceId + "/file1");
//        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
//        status = this.client.executeMethod(put);
//        assertEquals(HttpStatus.SC_CREATED, status);

//        //Are you sure it's there ????
//        GetMethod get = new GetMethod(this.root + testResourceId + "/file1");
//        status = client.executeMethod(get);
//        assertEquals(HttpStatus.SC_OK, status);
//        assertEquals("foo", get.getResponseBodyAsString());

//        put = new PutMethod(this.root + "testResourceId/file2");
//        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
//        status = this.client.executeMethod(put);
//        assertEquals(HttpStatus.SC_CREATED, status);
    }

    boolean resourceExists(String resource) throws IOException, DavException {
        try {
            MultiStatus multiStatus = this.getProperty(resource, DavPropertyName.DISPLAYNAME, false);
            MultiStatusResponse[] responses = multiStatus.getResponses();
            String value = null;
//        for (MultiStatusResponse r : responses) {
            DavPropertySet allProp = getProperties(responses[0]);
            DavPropertyIterator iter = allProp.iterator();
            while (iter.hasNext()) {
                DavProperty<?> p = iter.nextProperty();
                assertNotNull(p.getValue());
                value = (String) p.getValue();
                System.err.println(p.getName() + ":" + p.getValue());
                break;
            }
            if (value != null) {
                return true;
            } else {
                return false;
            }
        } catch (DavException ex) {
            if (ex.getMessage().contains("Not Found")) {
                return false;
            } else {
                throw ex;
            }
        }
    }

    void deleteResource(String resource, boolean mustSucceed) throws IOException {
        DeleteMethod delete = new DeleteMethod(resource);
        int status = this.client.executeMethod(delete);
        if (mustSucceed) {
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
        } else {
            assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);
        }
    }

    public void checkLogicalDataWrapped(TestREST.LogicalDataWrapped ldw) {

        assertNotNull(ldw.path);
        assertTrue((ldw.logicalData.uid != 0));
        if (ldw.logicalData.type.equals("logical.file")) {
            assertTrue((ldw.logicalData.pdriGroupId != 0));
            assertFalse(ldw.pdriList.isEmpty());
            assertNotNull(ldw.logicalData.contentTypesAsString);
            for (TestREST.PDRIDesc pdri : ldw.pdriList) {
                assertNotNull(pdri.name);
                assertNotNull(pdri.password);
                assertNotNull(pdri.resourceUrl);
                assertNotNull(pdri.username);
            }
        }

        assertNotNull(ldw.logicalData.createDate);
        assertTrue((ldw.logicalData.createDate != 0));
        assertNotNull(ldw.logicalData.modifiedDate);
        assertTrue((ldw.logicalData.modifiedDate != 0));
        assertNotNull(ldw.logicalData.name);

        for (TestREST.Permissions perm : ldw.permissions) {
            assertNotNull(perm.owner);
        }
    }

    public void postFile(File file, String url) throws IOException {

        System.err.println("post:" + file.getName() + " to: " + url);
        PostMethod method = new PostMethod(url);

        Part[] parts = {
            new StringPart("param_name", "value"),
            new FilePart(file.getName(), file)
        };

        MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts, method.getParams());
        method.setRequestEntity(requestEntity);

        int status = client.executeMethod(method);
        assertTrue(status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK);

//        HttpPost httppost = new HttpPost(uri.toASCIIString());
//        MultipartEntity mpEntity = new MultipartEntity();
//        ContentBody cbFile = new FileBody(file, "image/jpeg");
//        mpEntity.addPart("userfile", cbFile);
//
//
//        httppost.setEntity(mpEntity);
//        System.out.println("executing request " + httppost.getRequestLine());
//        CloseableHttpResponse response = httpclient.execute(httppost);
//
//
//        System.out.println(response.getStatusLine());
    }

    public String getChecksum(File file, String algorithm) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest md = MessageDigest.getInstance(algorithm);


        FileInputStream fis = new FileInputStream(file);
        byte[] dataBytes = new byte[1024];

        int nread = 0;

        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };

        byte[] mdbytes = md.digest();

        //convert the byte to hex format
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }

    public String checkChecksum(InputStream is) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = is.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public void setProperty(String resource, DavProperty property, boolean mustSucceed) throws IOException {
        DavPropertyNameSet commentNameSet = new DavPropertyNameSet();
        DavPropertySet descriptionSet = new DavPropertySet();
        descriptionSet.add(property);
        PropPatchMethod proPatch = new PropPatchMethod(resource, descriptionSet, commentNameSet);
        int status = client.executeMethod(proPatch);
        if (mustSucceed) {
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);
        }
    }

    File createRandomFile(String string, int sizeInMB) throws FileNotFoundException, IOException {
        File file = new File("/tmp/" + TestSettings.TEST_FILE_NAME1);


        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[1024 * 1024]; //1MB
        Random r = new Random();

        for (int i = 0; i < 2; i++) {
            r.nextBytes(buffer);
            fos.write(buffer);
            if (i % 100 == 0) {
                System.err.println(i + " of " + sizeInMB);
            }
        }
        fos.flush();
        fos.close();
        return file;
    }

    File DownloadFile(String resource, String dest, boolean mustSucceed) throws IOException {
        GetMethod get = new GetMethod(resource);
        client.executeMethod(get);
        int status = get.getStatusCode();
        if (mustSucceed) {
            assertEquals(HttpStatus.SC_OK, status);
        }
        InputStream in = get.getResponseBodyAsStream();
        File fromLob = new File(dest);
        FileOutputStream fos = new FileOutputStream(fromLob);

        byte buf[] = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }
        fos.close();
        in.close();
        return fromLob;
    }

    void saveFile(InputStream responseBodyAsStream, String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    List<File> unzipFile(File inFile) throws FileNotFoundException, IOException {
        ZipFile zipFile = new ZipFile(inFile);
        Enumeration<?> enu = zipFile.entries();
        List<File> files = new ArrayList();
        while (enu.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enu.nextElement();

            String name = zipEntry.getName();
            long size = zipEntry.getSize();
            long compressedSize = zipEntry.getCompressedSize();
            System.out.printf("name: %-20s | size: %6d | compressed size: %6d\n",
                    name, size, compressedSize);

            File file = new File(name);
            if (name.endsWith("/")) {
                file.mkdirs();
                continue;
            }

            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            InputStream is = zipFile.getInputStream(zipEntry);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = is.read(bytes)) >= 0) {
                fos.write(bytes, 0, length);
            }
            is.close();
            fos.close();
            if (file.isFile()) {
                files.add(file);
            }

        }
        zipFile.close();
        return files;
    }

    List<String> lobProperty2List(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        return Arrays.asList(value.split("\\s*,\\s*"));
    }

    void checkPermissions(List<TestREST.LogicalDataWrapped> list, TestREST.Permissions perm) {
        assertNotNull(list);
        for (TestREST.LogicalDataWrapped ldw : list) {
            Set<TestREST.Permissions> perSet = ldw.permissions;
            for (TestREST.Permissions p : perSet) {
                assertEquals(perm.owner, p.owner);
                boolean foundWrite = false, foundRead = false;
                for (String s : p.read) {
                    for (String init : perm.read) {
                        if (s.equals(init)) {
                            foundRead = true;
                            break;
                        }
                    }
                }
                for (String s : p.write) {
                    for (String init : perm.write) {
                        if (s.equals(init)) {
                            foundWrite = true;
                            break;
                        }
                    }
                }
                assertTrue(foundRead);
                assertTrue(foundWrite);
            }
        }
    }
}

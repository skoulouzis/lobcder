package nl.uva.cs.lobcder.catalogue.beans;

import io.milton.http.LockInfo;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.resources.PDRIDescr;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

/**
 * User: dvasunin Date: 24.02.14 Time: 17:28 To change this template use File |
 * Settings | File Templates.
 */
@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemBean {

    /**
     * @return the uid
     */
    public Long getUid() {
        return uid;
    }

    /**
     * @param uid the uid to set
     */
    public void setUid(Long uid) {
        this.uid = uid;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the parentRef
     */
    public Long getParentRef() {
        return parentRef;
    }

    /**
     * @param parentRef the parentRef to set
     */
    public void setParentRef(Long parentRef) {
        this.parentRef = parentRef;
    }

    /**
     * @return the createDate
     */
    public XMLGregorianCalendar getCreateDate() {
        return createDate;
    }

    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(XMLGregorianCalendar createDate) {
        this.createDate = createDate;
    }

    /**
     * @return the modifiedDate
     */
    public XMLGregorianCalendar getModifiedDate() {
        return modifiedDate;
    }

    /**
     * @param modifiedDate the modifiedDate to set
     */
    public void setModifiedDate(XMLGregorianCalendar modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @return the length
     */
    public Long getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(Long length) {
        this.length = length;
    }

    /**
     * @return the contentTypesAsString
     */
    public String getContentTypesAsString() {
        return contentTypesAsString;
    }

    /**
     * @param contentTypesAsString the contentTypesAsString to set
     */
    public void setContentTypesAsString(String contentTypesAsString) {
        this.contentTypesAsString = contentTypesAsString;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the lock
     */
    public LockTokenBean getLock() {
        return lock;
    }

    /**
     * @param lock the lock to set
     */
    public void setLock(LockTokenBean lock) {
        this.lock = lock;
    }

    /**
     * @return the preference
     */
    public Collection<StorageSiteBean> getPreference() {
        return preference;
    }

    /**
     * @param preference the preference to set
     */
    public void setPreference(Collection<StorageSiteBean> preference) {
        this.preference = preference;
    }

    /**
     * @return the permissions
     */
    public Permissions getPermissions() {
        return permissions;
    }

    /**
     * @param permissions the permissions to set
     */
    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the dri
     */
    public DriBean getDri() {
        return dri;
    }

    /**
     * @param dri the dri to set
     */
    public void setDri(DriBean dri) {
        this.dri = dri;
    }

    public static enum Type {
        FILE, FOLDER, NULLOCKED
    };
    private Long uid;
    private Type type;
    private String name;
    private Long parentRef;
    private XMLGregorianCalendar createDate;
    private XMLGregorianCalendar modifiedDate;
    private Long length;
    @XmlElement(name = "contentType")
    private String contentTypesAsString;
    private String description;
    private LockTokenBean lock;

    private Collection<StorageSiteBean> preference;
    private Permissions permissions;
    private String path;
    private DriBean dri;

    public void addContentType(String contentType) {
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        if (getContentTypesAsString() == null) {
            setContentTypesAsString(new String());
        }
        String ct[] = getContentTypesAsString().split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            String cont;
            if (getContentTypesAsString().isEmpty()) {
                cont = "," + contentType;
            } else {
                cont = getContentTypesAsString();
            }
            setContentTypesAsString(cont);
        }
    }

    public boolean isFolder() {
        return getType().equals(Type.FOLDER);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ItemBean)) {
            return false;
        }
        ItemBean other = (ItemBean) o;
        if (other.getUid().equals(getUid())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }

    /// test
    public static void main(String[] args) {
        try {
            DatatypeFactory df = DatatypeFactory.newInstance();
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            ItemBean ldb = new ItemBean();
            ldb.setUid(123L);
            ldb.setCreateDate(df.newXMLGregorianCalendar(gregorianCalendar));
            ldb.setType(Type.FILE);
            ldb.setName("Name");
            ldb.setParentRef(45L);
            Permissions p = new Permissions();
            p.setOwner("Dima");
            p.getRead().add("group_read1");
            p.getRead().add("group_read2");
            p.getWrite().add("group_write1");
            ldb.setPermissions(p);
            ldb.setParentRef(46L);
            ldb.addContentType("application/xml");
            ldb.setPath("/qwa/qwe");
            List<PDRIDescr> pris = new ArrayList<>();

            //pdril.getPdriList().add(new PdriBean("name", "url", "username", "password", false, null, false));
            //pdril.getPdriList().add(new PdriBean("name2","url", "username", "password", false, null, false));
            //ldb.setPdriList(pdril);
            LockInfo lockInfo = new LockInfo(LockInfo.LockScope.EXCLUSIVE,
                    LockInfo.LockType.WRITE, "user",
                    LockInfo.LockDepth.INFINITY);
//            LockTokenBean lockToken = new LockTokenBean();
            //lockToken.setLocktoken("token-name");
            //lockToken.setLockInfo(lockInfo);
            //lockToken.setLockedUntil(df.newXMLGregorianCalendar(gregorianCalendar));
//            ldb.setLock(lockToken);
            DriBean dri = new DriBean();
            dri.setStatus(DriBean.Status.unavailable);
            ldb.setDri(dri);
            JAXBContext context = JAXBContext.newInstance(ItemBean.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(ldb, System.err);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

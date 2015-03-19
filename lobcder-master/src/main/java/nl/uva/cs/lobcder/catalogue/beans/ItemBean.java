package nl.uva.cs.lobcder.catalogue.beans;

import io.milton.http.LockInfo;
import io.milton.http.LockToken;
import lombok.Data;
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
 * User: dvasunin
 * Date: 24.02.14
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 */
@Data
@XmlRootElement(name = "item")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemBean {

    public static enum Type{
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
        if (contentTypesAsString == null) {
            contentTypesAsString = new String();
        }
        String ct[] = contentTypesAsString.split(",");
        if (!Arrays.asList(ct).contains(contentType)) {
            contentTypesAsString += contentTypesAsString.isEmpty() ? contentType : ("," + contentType);
        }
    }

    public boolean isFolder() {
        return type.equals(Type.FOLDER);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ItemBean)) return false;
        ItemBean other = (ItemBean) o;
        if (other.uid.equals(uid)) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return uid.hashCode();
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

package com.bradmcevoy.http;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import junit.framework.TestCase;

public class TestUtils extends TestCase {

    public void test() throws NotAuthorizedException, BadRequestException {
        ColRes col1 = new ColRes("col1");
        ColRes col2 = new ColRes("col2");
        col1.children.put("col2",col2);
        Res page = new Res("page");
        col2.children.put("page",page);
        
        Path path = Path.path("col2/page");
        Resource r = Utils.findChild(col1, path);
        assertEquals(page, r);
    }

    public void testGetProtocol() {
        String url = "http://abc.com/aaa";
        assertEquals("http", Utils.getProtocol(url));

        url = "http://abc.com:80/aaa";
        assertEquals("http", Utils.getProtocol(url));

        url = "https://abc.com/aaa";
        assertEquals("https", Utils.getProtocol(url));

    }

    class Res implements Resource {

        final String name;
        final UUID id;

        public Res(String name) {
            this.name = name;
            this.id = UUID.randomUUID();
        }

        
        public String getUniqueId() {
            return id.toString();
        }
        
        
                       
        
        public String getName() {
            return name;
        }

        
        public Object authenticate(String user, String password) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public boolean authorise(Request request, Method method, Auth auth) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public String getRealm() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public Date getModifiedDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public Long getContentLength() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public String getContentType(String accepts) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public String checkRedirect(Request request) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        public int compareTo(Resource o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }        
    }
    
    class ColRes extends Res implements CollectionResource {

        public Map<String,Resource> children  = new HashMap<String,Resource>();

        public ColRes(String name) {
            super(name);
        }
        
        
        
        
        public List<? extends Resource> getChildren() {
            return new ArrayList<Resource>(children.values());
        }

        
        public Resource child(String childName) {
            return children.get(childName);
        }                        
    }
}

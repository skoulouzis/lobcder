package com.bradmcevoy.http.values;

import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.webdav.WebDavProtocol.SupportedLocks;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class SupportedLockValueWriterTest extends TestCase {
	
	SupportedLockValueWriter valueWriter;
	
	public SupportedLockValueWriterTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		valueWriter = new SupportedLockValueWriter();
	}


	public void testWriteValue() {
		PropFindableResource res = null;
		SupportedLocks locks = new SupportedLocks(res);
		
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XmlWriter xmlWriter = new XmlWriter(out);
		LockInfo lockInfo = new LockInfo(LockInfo.LockScope.EXCLUSIVE, LockInfo.LockType.READ, null, LockInfo.LockDepth.ZERO);
		LockTimeout lockTimeout = new LockTimeout(1000l);
		LockToken token = new LockToken("abc123", lockInfo, lockTimeout);
		Map<String,String> prefixes = new HashMap<String, String>();
		
		valueWriter.writeValue(xmlWriter, "uri", "ns", "aName", locks, "/test", prefixes);
		
		xmlWriter.flush();
		String xml = out.toString();
		System.out.println(xml);
		System.out.println("---------------------------------");		
		
		// Should look like this:
//<D:supportedlock>
//<D:lockentry>
//<D:lockscope><D:exclusive/></D:lockscope>
//<D:locktype><D:write/></D:locktype>
//</D:lockentry>
//</D:supportedlock>
	}

}

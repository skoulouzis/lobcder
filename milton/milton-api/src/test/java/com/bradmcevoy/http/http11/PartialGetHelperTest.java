package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.entity.PartialEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import static org.easymock.classextension.EasyMock.*;

/**
 *
 * @author brad
 */
public class PartialGetHelperTest extends TestCase {
	
	PartialGetHelper partialGetHelper;
	Http11ResponseHandler responseHandler;
	
	public PartialGetHelperTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		responseHandler = createMock(Http11ResponseHandler.class);
		partialGetHelper = new PartialGetHelper(responseHandler);
	}

	public void testGetRange_null() {
		List<Range> ranges = partialGetHelper.getRanges(null);
		assertNull(ranges);
		ranges = partialGetHelper.getRanges("");
		assertNull(ranges);
	}
	
	public void testGetRange_single() {
		List<Range> ranges = partialGetHelper.getRanges("bytes=0-499");
		assertNotNull(ranges);
		assertEquals(1, ranges.size());
		Range r = ranges.get(0);
		assertEquals(0, r.getStart());
		assertEquals(499, r.getFinish());
	}
	
	public void testGetRange_multi() {
		List<Range> ranges = partialGetHelper.getRanges("bytes=0-499,1000-1500,2000-2500");
		assertNotNull(ranges);
		assertEquals(3, ranges.size());
		assertEquals(0, ranges.get(0).getStart());
		assertEquals(499, ranges.get(0).getFinish());
		assertEquals(1000, ranges.get(1).getStart());
		assertEquals(1500, ranges.get(1).getFinish());
		assertEquals(2000, ranges.get(2).getStart());
		assertEquals(2500, ranges.get(2).getFinish());

		
	}

	public void testGetRanges() {
	}

	public void testSendPartialContent() throws Exception {
	}

	public void testGetMaxMemorySize() {
	}

	public void testSetMaxMemorySize() {
	}

	public void testSendBytes_Under1k() throws Exception {
		long length = 500;
		byte[] buf = new byte[1000];
		Arrays.fill(buf, (byte)3);
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		PartialEntity.sendBytes(in, out, length);
		
		assertEquals(500, out.toByteArray().length);
		
	}
	
	public void testSendBytes_Over1k() throws Exception {
		long length = 5000;
		byte[] buf = new byte[10000];
		Arrays.fill(buf, (byte)3);
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
        PartialEntity.sendBytes(in, out, length);
		
		assertEquals(5000, out.toByteArray().length);
		
	}	
	
	public void testWriteRanges() throws IOException {
		long length = 5000;
		byte[] buf = new byte[10000];
		for( int i=0; i<5; i++) {
			char ch = (char)(65 + i);
			Arrays.fill(buf,i*1000, (i+1)*1000, (byte)ch);
		}
		ByteArrayInputStream in = new ByteArrayInputStream(buf);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		List<Range> ranges = new ArrayList<Range>();
		ranges.add(new Range(500, 1000));
		ranges.add(new Range(2000, 2500));
		ranges.add(new Range(3000, 3500));
		
        PartialEntity.writeRanges(in, ranges, out);
		
		assertEquals(1500, out.toByteArray().length);

	}
	
}

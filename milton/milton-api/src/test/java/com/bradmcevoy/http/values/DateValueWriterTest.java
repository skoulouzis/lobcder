package com.bradmcevoy.http.values;

import com.bradmcevoy.http.XmlWriter;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author brad
 */
public class DateValueWriterTest extends TestCase {
	
	
	DateValueWriter	dateValueWriter;
	
	@Override
	protected void setUp() throws Exception {
		dateValueWriter = new DateValueWriter();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSupports_NormalDate() {
		assertTrue( dateValueWriter.supports(null, null, Date.class) );
	}
	
	public void testSupports_SqlDate() {
		assertTrue( dateValueWriter.supports(null, null, java.sql.Date.class) );
	}
	
	public void testSupports_SqlTimestamp() {
		assertTrue( dateValueWriter.supports(null, null, Timestamp.class) );
	}	
	
	public void testSupports_FalseForString() {
		assertFalse( dateValueWriter.supports(null, null, String.class) );
	}		


}

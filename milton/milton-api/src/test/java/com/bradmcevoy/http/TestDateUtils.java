package com.bradmcevoy.http;

import com.bradmcevoy.http.DateUtils.DateParseException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import junit.framework.TestCase;

public class TestDateUtils extends TestCase {
    public TestDateUtils() {
    }
    
    public void test() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy hh:mm");
        Date dt = sdf.parse("1-1-2007 19:03");        
        System.out.println("parsed: " + dt);
        String s = DateUtils.formatDate(dt);
        System.out.println("formatted to: " + s);
    }

    public void testParseNormal() throws DateParseException {
        Date dt = DateUtils.parseDate( "Sun, 28 Mar 2010 01:00:00 GMT");
        System.out.println( dt.getTime() );
        assertEquals( 1269738000000l, dt.getTime());
    }

    /**
     * See http://www.ettrema.com:8080/browse/MIL-60
     *
     * @throws com.bradmcevoy.http.DateUtils.DateParseException
     */
    public void testParseWithoutSeconds() throws DateParseException {
        Date dt = DateUtils.parseDate( "Sun, 28 Mar 2010 01:00 GMT");
        System.out.println( dt.getTime() );
        assertEquals( 1269738000000l, dt.getTime());
    }

    public void testParseHeaderFormat() throws DateParseException {
        Date dt = DateUtils.parseDate("2010-04-11 12:00:00");
        System.out.println("dt: " + dt);
    }
}

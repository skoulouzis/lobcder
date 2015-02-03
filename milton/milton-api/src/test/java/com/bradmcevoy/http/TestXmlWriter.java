package com.bradmcevoy.http;

import org.apache.commons.io.output.ByteArrayOutputStream;
import junit.framework.TestCase;

public class TestXmlWriter extends TestCase {
    public TestXmlWriter() {
    }

    public void test() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter w = new XmlWriter(out);
        XmlWriter.Element el = w.begin("a").writeAtt("att","val");
        el.open();
        el.writeText("abc");
        el.close();
        w.flush();
        String s = out.toString();
        System.out.println("actual..");
        System.out.println(s);
        String expected = "<a att=\"val\">\nabc</a>\n";
        System.out.println("expected..");
        System.out.println(expected);
//        assertEquals(expected,s);
    }

    public void testNested() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter w = new XmlWriter(out);
        w.begin("a")
            .begin("b")
                .prop("b1", "b1_val")
//                .prop("b2", "b2_val")
            .close()
        .close();
//            .prop("a1","a1_val");
        w.flush();
        String s = out.toString();
        System.out.println("actual: \n" + s);
    }
}

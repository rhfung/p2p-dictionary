package com.rhfung.P2PDictionary;

import com.rhfung.P2PDictionary.URLUtils;
import com.sun.xml.internal.ws.api.message.ExceptionHasMessage;
import junit.framework.TestCase;

public class URLUtilsTest extends TestCase {

    public void testEncode() throws Exception {
        TestCase.assertEquals("/", URLUtils.URLEncode("/"));
        TestCase.assertEquals("/base", URLUtils.URLEncode("/base"));
        TestCase.assertEquals("/base%5C%3A", URLUtils.URLEncode("/base\\:"));
        TestCase.assertEquals("/base/subpath", URLUtils.URLEncode("/base/subpath"));

        TestCase.assertEquals("/base", URLUtils.URLEncode("/base/"));
        TestCase.assertEquals(null, URLUtils.URLEncode(null));
        TestCase.assertEquals("", URLUtils.URLEncode(""));
    }

    public void testDecode() throws Exception {
        TestCase.assertEquals("/", URLUtils.URLDecode("/"));
        TestCase.assertEquals("/base", URLUtils.URLDecode("/base"));
        TestCase.assertEquals("/base\\:", URLUtils.URLDecode("/base%5C%3A"));
        TestCase.assertEquals("/base/subpath", URLUtils.URLDecode("/base/subpath"));

        TestCase.assertEquals("/base", URLUtils.URLDecode("/base/"));
        TestCase.assertEquals(null, URLUtils.URLDecode(null));
        TestCase.assertEquals("", URLUtils.URLDecode(""));
    }

    public void testHeaderParsing() throws Exception {
        String parts[] = URLUtils.splitFrontEnd3("GET /index.html HTTP/1.1");

        TestCase.assertEquals("GET", parts[0]);
        TestCase.assertEquals("/index.html", parts[1]);
        TestCase.assertEquals("HTTP/1.1", parts[2]);

        parts = URLUtils.splitFrontEnd3("HTTP/1.1 200 OK");

        TestCase.assertEquals("HTTP/1.1", parts[0]);
        TestCase.assertEquals("200", parts[1]);
        TestCase.assertEquals("OK", parts[2]);

        parts = URLUtils.splitFrontEnd3("HTTP/1.1 305 Use Proxy");

        TestCase.assertEquals("HTTP/1.1", parts[0]);
        TestCase.assertEquals("305", parts[1]);
        TestCase.assertEquals("Use Proxy", parts[2]);

    }
}

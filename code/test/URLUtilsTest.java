import com.rhfung.P2PDictionary.URLUtils;
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
}

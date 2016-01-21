import com.rhfung.P2PDictionary.NetworkUtil;
import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.logging.LogInstructions;
import junit.framework.TestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by richard on 1/20/16.
 */
public class IntegrationTest extends TestCase {
    P2PDictionary dict1;
    P2PDictionary dict2;
    int dict1Port;
    int dict2Port;

    protected void setUp() throws Exception {
        super.setUp();
        dict1Port = NetworkUtil.freePort(9000);
        dict1 =  P2PDictionary.builder()
                .setPort(dict1Port)
                .setNamespace("test")
                .setLogLevel(System.out, LogInstructions.INFO)
                .build();

        dict2Port = NetworkUtil.freePort(dict1Port + 1);
        dict2 = P2PDictionary.builder()
                .setPort(dict2Port)
                .setNamespace("test")
                .setLogLevel(System.out, LogInstructions.INFO)
                .build();

        dict1.addSubscription("*");
        dict2.addSubscription("*");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        dict1.close();
        dict2.close();
    }

    public void testDisconnectedPeers() {
        dict1.put("value", "test");

        Object retValue = dict1.tryGetValue("value", 1000, null);
        TestCase.assertEquals("test", retValue);

        retValue = dict2.tryGetValue("value", 1000, null);
        TestCase.assertEquals(null, retValue);
    }

    public void testTwoPeers() {
        dict1.put("value", "test");
        try {
            dict2.openClient(InetAddress.getByName("127.0.0.1"), dict1Port);
        } catch (UnknownHostException ex) {
            TestCase.fail("Unable to test network");
        }

        Object retValue = dict1.tryGetValue("value", 1000, null);
        TestCase.assertEquals("test", retValue);

        retValue = dict2.tryGetValue("value", 5000, null);
        TestCase.assertEquals("test", retValue);

        dict2.put("response", "ping");
        retValue = dict1.tryGetValue("response", 5000, null);
        TestCase.assertEquals("ping", retValue);
    }

    public void testTwoPeersReversed() {
        dict1.put("value", "test");
        try {
            dict1.openClient(InetAddress.getByName("127.0.0.1"), dict2Port);
        } catch (UnknownHostException ex) {
            TestCase.fail("Unable to test network");
        }

        Object retValue = dict1.tryGetValue("value", 1000, null);
        TestCase.assertEquals("test", retValue);

        retValue = dict2.tryGetValue("value", 5000, null);
        TestCase.assertEquals("test", retValue);

        dict2.put("response", "ping");
        retValue = dict1.tryGetValue("response", 5000, null);
        TestCase.assertEquals("ping", retValue);

    }

    public void testTwoPeersTwoConnections() {
        dict1.put("value", "test");

        try {
            dict2.openClient(InetAddress.getByName("127.0.0.1"), dict1Port);
            dict1.openClient(InetAddress.getByName("127.0.0.1"), dict2Port);
        } catch (UnknownHostException ex) {
            TestCase.fail("Unable to test network");
        }

        Object retValue = dict1.tryGetValue("value", 1000, null);
        TestCase.assertEquals("test", retValue);

        retValue = dict2.tryGetValue("value", 5000, null);
        TestCase.assertEquals("test", retValue);

        dict2.put("response", "ping");
        retValue = dict1.tryGetValue("response", 5000, null);
        TestCase.assertEquals("ping", retValue);
    }
}

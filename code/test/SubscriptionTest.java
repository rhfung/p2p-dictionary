import com.rhfung.P2PDictionary.ISubscriptionChanged;
import com.rhfung.P2PDictionary.Subscription;
import com.rhfung.P2PDictionary.SubscriptionInitiator;
import junit.framework.TestCase;

/**
 * Created by richard on 1/19/16.
 */
public class SubscriptionTest extends TestCase {

    Subscription subscription;

    public SubscriptionTest() {
        subscription = new Subscription(new ISubscriptionChanged() {

            @Override
            public void onAddedSubscription(Subscription s, String wildcardString, SubscriptionInitiator initiator) {

            }

            @Override
            public void onRemovedSubscription(Subscription s, String wildcardString) {

            }
        });
    }

    public void testSubscriptionAdd() {
        TestCase.assertEquals(0, subscription.getSubscriptionList().size());

        subscription.AddSubscription("/test/this/key", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(1, subscription.getSubscriptionList().size());
        TestCase.assertEquals(true, subscription.isSubscribed("/test/this/key"));

        subscription.AddSubscription("/test/this/key", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(1, subscription.getSubscriptionList().size());
    }

    public void testSubscriptionWildcardMatch() {
        subscription.AddSubscription("/test/this/key", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(1, subscription.getSubscriptionList().size());
        TestCase.assertEquals(true, subscription.isSubscribed("/test/this/key"));

        subscription.AddSubscription("/test/this/*", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(2, subscription.getSubscriptionList().size());
        TestCase.assertEquals(true, subscription.isSubscribed("/test/this/key"));
        TestCase.assertEquals(true, subscription.isSubscribed("/test/this/any"));
        TestCase.assertEquals(false, subscription.isSubscribed("/test/this"));

        subscription.AddSubscription("/some?", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(false, subscription.isSubscribed("/some"));
        TestCase.assertEquals(true, subscription.isSubscribed("/some1"));
        TestCase.assertEquals(true, subscription.isSubscribed("/somed"));

        subscription.AddSubscription("/other#", SubscriptionInitiator.AutoAddKey);
        TestCase.assertEquals(false, subscription.isSubscribed("/other"));
        TestCase.assertEquals(true, subscription.isSubscribed("/other1"));
        TestCase.assertEquals(false, subscription.isSubscribed("/otherd"));

    }
}

import java.util.Scanner;

import com.rhfung.P2PDictionary.*;
import com.rhfung.P2PDictionary.callback.IDictionaryCallback;
import com.rhfung.P2PDictionary.subscription.SubscriptionEventArgs;
import com.rhfung.P2PDictionary.subscription.SubscriptionException;


public class TestRun extends IDictionaryCallback {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Server started on port 3333");
        System.out.println("Open your web browser to http://localhost:3333 and verify that dictionary entries" +
                "\nare properly served from the P2P Dictionary server");

        P2PDictionary node = P2PDictionary.builder()
                .setDescription("test")
                .setNamespace("test")
                .setPort(3333)
                .setServerMode(P2PDictionaryServerMode.AutoRegister)
                .setClientMode(P2PDictionaryClientMode.AutoConnect)
                .setClientSearchTimespan(1500)
                .setCallback(new TestRun())
                .build();

        /*
         * The dictionary's keys are programmatically assigned using get/set operations,
         * similar to a regular dictionary.
         */

        node.put("message1", "hello world");
        node.put("message2", 2.3);
        node.put("message3", java.awt.Color.BLACK);

        byte[] byteArray = new byte[3];
        byteArray[0] = 45;
        byteArray[1] = 46;
        byteArray[2] = 47;
        node.put("message4", byteArray);

        node.put("message5", 3210);
        node.put("message6", null);
        node.put("message7", true);

        SomeClass toSend = new SomeClass();
        toSend.first = "firstvalue";
        toSend.second = "secondvalue";
        toSend.setHidden("hiddenvalue");
        node.put("message8", toSend);

        /*
         * Some waiting event to keep the server open for demonstration only.
         */

        System.out.print("Press Enter to show saved values...");

        Scanner sc = new Scanner(System.in);
        sc.nextLine();

        /*
         * Reading from the dictionary is straightforward.
         */

        try {
            System.out.println("message1 is " + node.get("message1"));
            System.out.println("message2 is " + node.get("message2"));
            System.out.println("message3 is " + node.get("message3"));
            byte[] msg4 = (byte[]) node.get("message4");
            System.out.print("message4 is [");
            for (byte b: msg4) {
                System.out.print(b);
                System.out.print(" ");
            }
            System.out.println("]");
            System.out.println("message5 is " + node.get("message5"));
            System.out.println("message6 is " + node.get("message6"));
            System.out.println("message7 is " + node.get("message7"));
            System.out.println("message8 is " + node.get("message8"));
        } catch (SubscriptionException ex) {
            // do nothing
        }

        System.out.println("closing...");
        node.close();
        System.out.println("goodbye");
    }

    /*
     * The dictionary can act in pub-sub mode. Here are the subscription events.
     */

    @Override
    public void SubscriptionChanged(SubscriptionEventArgs e) {
        System.out.println("Subscription " + e.getReason() + " " + e.getSubscriptionPattern());
    }

    @Override
    public void Notification(NotificationEventArgs e) {
        System.out.println("Notification " +  e.getReason()  + " key=" + e.getKey() );
    }

    @Override
    public void Connected(ConnectionEventArgs e) {
        System.out.println("Connected");
    }

    @Override
    public void Disconnected(ConnectionEventArgs e) {
        System.out.println("Disconnected");
    }

    @Override
    public void ConnectionFailure(ConnectionEventArgs e) {
        System.out.println("Connection Failure");
    }
}

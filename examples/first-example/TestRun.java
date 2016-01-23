import java.io.PrintStream;
import java.util.Scanner;

import com.rhfung.P2PDictionary.*;
import com.rhfung.P2PDictionary.subscription.SubscriptionException;
import com.rhfung.logging.LogInstructions;


public class TestRun {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Server started on port 3333");
        System.out.println("Open your web browser to http://localhost:3333 and verify that dictionary entries\nare properly served from the P2P Dictionary server");

        P2PDictionary node = P2PDictionary.builder()
                .setDescription("test")
                .setNamespace("test")
                .setPort(3333)
                .setServerMode(P2PDictionaryServerMode.AutoRegister)
                .setClientMode(P2PDictionaryClientMode.AutoConnect)
                .setClientSearchTimespan(1500)
                .setLogLevel(System.out, LogInstructions.DEBUG)
                .build();

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

        System.out.print("Press any key to show saved values...");

        Scanner sc = new Scanner(System.in);
        sc.nextLine();

        try {
            System.out.println("message1 is " + node.get("message1"));
            System.out.println("message2 is " + node.get("message2"));
            System.out.println("message3 is " + node.get("message3"));
            System.out.println("message4 is " + node.get("message4"));
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

}

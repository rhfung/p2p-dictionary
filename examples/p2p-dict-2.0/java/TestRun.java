import java.io.PrintStream;
import java.util.Scanner;

import com.rhfung.P2PDictionary.*;


public class TestRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			System.out.println("Server started on port 3333");
			System.out.println("Open your web browser to http://localhost:3333 and verify that dictionary entries\nare properly served from the P2P Dictionary server");
		
			P2PDictionary node = new P2PDictionary("test", 3333, "test", P2PDictionaryServerMode.AutoRegister, P2PDictionaryClientMode.AutoConnect, 1500);
			
			node.setDebugBuffer(new PrintStream(System.out), 0,true );
			
			byte[] byteArray = new byte[3];
			byteArray[0] = 45;
			byteArray[1] = 46;
			byteArray[2] = 47;
			
			SomeClass toSend = new SomeClass();
			toSend.first = "firstvalue";
			toSend.second = "secondvalue";
			toSend.setHidden("hiddenvalue");

			node.put(Integer.toString(node.getLocalID()) + "/message0", "hello world");
			node.put(Integer.toString(node.getLocalID()) + "/message1", 2.3);
			node.put(Integer.toString(node.getLocalID()) + "/message2", java.awt.Color.BLACK);
			node.put(Integer.toString(node.getLocalID()) + "/message3", byteArray);
			node.put(Integer.toString(node.getLocalID()) + "/message4", 3210);
			node.put(Integer.toString(node.getLocalID()) + "/message5", null);
			node.put(Integer.toString(node.getLocalID()) + "/message6", true);
			node.put(Integer.toString(node.getLocalID()) + "/message7", toSend);

			Scanner sc = new Scanner(System.in);
			sc.nextLine();

			System.out.println("closing...");
			node.close();
			System.out.println("goodbye");
	}		

}

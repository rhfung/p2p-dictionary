import java.io.PrintStream;
import java.util.Scanner;

import com.rhfung.P2PDictionary.*;


public class TestRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	
			P2PDictionary node = new P2PDictionary("test", 3333, "test", P2PDictionaryServerMode.AutoRegister, P2PDictionaryClientMode.AutoConnect, 1500);
			
			node.setDebugBuffer(new PrintStream(System.out), 0,true );

			node.put(Integer.toString(node.getLocalID()) + "/message0", "hello world");
			node.put(Integer.toString(node.getLocalID()) + "/message1", "how are you?");
			node.put(Integer.toString(node.getLocalID()) + "/message2", java.awt.Color.BLACK);

			Scanner sc = new Scanner(System.in);
			sc.nextLine();

			System.out.println("closing...");
			node.close();
			System.out.println("goodbye");


	
	}		

}

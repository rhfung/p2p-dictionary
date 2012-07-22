import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.P2PDictionary.P2PDictionaryClientMode;
import com.rhfung.P2PDictionary.P2PDictionaryServerMode;


public class MainObject {
	public static final void main(String[] args)
	{
		P2PDictionary dict = new P2PDictionary("Local File Transfer", 8765, "lft", P2PDictionaryServerMode.AutoRegister,
				P2PDictionaryClientMode.AutoConnect, 1500);
		
		dict.setDebugBuffer(System.out, 1, true);
		dict.setDefaultKey("index.html");
		dict.put("index.html", new com.rhfung.P2PDictionary.MIMEByteObject("text/html", getFileInPackage("/app-index.html")));
		dict.put("backbone.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/backbone.js")));
		dict.put("format.css", new com.rhfung.P2PDictionary.MIMEByteObject("text/css", getFileInPackage("/format.css")));
		dict.put("underscore.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/underscore.js")));
		dict.put("jquery-1.7.2.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/jquery-1.7.2.js")));
		
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Closing");
		dict.close();
	}
	
	public static byte[] getFileInPackage(String pathname)
	{
		InputStream stream = Thread.currentThread().getClass().getResourceAsStream(pathname);
		try {
			return IOUtils.toByteArray(stream);
		} catch (IOException e) {
			e.printStackTrace();

			return new byte[0];
		}
		
	}
}

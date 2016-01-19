import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.P2PDictionary.P2PDictionaryClientMode;
import com.rhfung.P2PDictionary.P2PDictionaryServerMode;


public class MainObject {
	public static final void main(String[] args)
	{
		System.out.println("Starting server on port 8765...");
		
		final P2PDictionary dict = (new P2PDictionary.Builder())
			.setDescription("Local File Transfer")
			.setPort(8765)
			.setNamespace("lft")
			.setClientSearchTimespan(1500)
			.build();
		
		dict.setDebugBuffer(System.out, 1, true);
		dict.setDefaultKey("index.html");
		dict.put("index.html", new com.rhfung.P2PDictionary.MIMEByteObject("text/html", getFileInPackage("/app-index.html")));
		dict.put("app-index.js", new com.rhfung.P2PDictionary.MIMEByteObject("text/html", getFileInPackage("/app-index.js")));
		dict.put("backbone.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/backbone.js")));
		dict.put("format.css", new com.rhfung.P2PDictionary.MIMEByteObject("text/css", getFileInPackage("/format.css")));
		dict.put("underscore.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/underscore.js")));
		dict.put("jquery-1.7.2.js", new com.rhfung.P2PDictionary.MIMEByteObject("application/javascript", getFileInPackage("/jquery-1.7.2.js")));

		Thread shutdown = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Closing server");
				dict.close();
			}
		});

		Runtime.getRuntime().addShutdownHook(shutdown);

		System.out.println("Started");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Closing server...");
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

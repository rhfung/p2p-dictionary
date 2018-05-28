import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import java.net.InetAddress;

import com.rhfung.P2PDictionary.P2PDictionary;


public class MainObject {
	public static final void main(String[] args)
	{
		System.out.println("Starting server on port 8800...");
		
		final P2PDictionary dict = P2PDictionary.builder()
			.setDescription("Local File Transfer")
			.setPort(8800)
			.setNamespace("lft")
			.setClientSearchTimespan(1500)
			.build();

		// Uncomment the following line to debug:
		// dict.setDebugBuffer(System.out, 1, true);

		dict.addSubscription("*");
		dict.setDefaultKey("index.html");
		dict.put("index.html", new MIMEByteObject("text/html", getFileInPackage("/app-index.html")));
		dict.put("app-index.js", new MIMEByteObject("text/html", getFileInPackage("/app-index.js")));
		dict.put("backbone.js", new MIMEByteObject("application/javascript", getFileInPackage("/backbone.js")));
		dict.put("format.css", new MIMEByteObject("text/css", getFileInPackage("/format.css")));
		dict.put("underscore.js", new MIMEByteObject("application/javascript", getFileInPackage("/underscore.js")));
		dict.put("jquery-1.7.2.js", new MIMEByteObject("application/javascript", getFileInPackage("/jquery-1.7.2.js")));

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

package com.rhfung.P2PDictionary;

//P2PDictionary
//Copyright (C) 2013, Richard H Fung (www.richardhfung.com)
//
//Permission is hereby granted to any person obtaining a copy of this software 
//and associated documentation files (the "Software"), to deal in the Software 
//for the sole purposes of PERSONAL USE. This software cannot be used in 
//products where commercial interests exist (i.e., license, profit from, or
//otherwise seek monetary value). The person DOES NOT HAVE the right to
//redistribute, copy, modify, merge, publish, sublicense, or sell this Software
//without explicit prior permission from the author, Richard H Fung.
//
//The above copyright notice and this permission notice shall be included 
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class NetworkUtil {
	private static final int MIN_PORT_NUMBER = 1;
	private static final int MAX_PORT_NUMBER = 65535;

	// response codes
	public static final String RESPONSE_STR_GOOD = "200";
	public static final String RESPONSE_STR_PROXY = "305";
	public static final String RESPONSE_STR_PROXY2 = "307";
	public static final String RESPONSE_STR_DELETED = "404";

	public static final int VALUE_GOOD = 200;
	public static final int VALUE_MOVED = 301;
	public static final int VALUE_PROXY = 305;
	public static final int VALUE_PROXY2 = 307;
	public static final int VALUE_BAD_REQUEST = 400;
	public static final int VALUE_FORBIDDEN = 403;
	public static final int VALUE_NOTFOUND = 404;
	public static final int VALUE_BADMETHOD = 405;
	public static final int VALUE_INTERNAL_SERVER_ERROR = 500;
	public static final int VALUE_NOT_IMPLEMENTED= 501;

	/**
	 * Returns a free port number at/above suggestedPort.
	 * @param suggestedPort
	 * @return
	 */
	public static int freePort(int suggestedPort)
	{
		while(!available(suggestedPort))
		{
			suggestedPort++;
		}
		return suggestedPort;
	}
	
	/**
	 * Checks to see if a specific port is available.
	 * http://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
	 * @param port the port to check for availability
	 */
	private static boolean available(int port) {
	    if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
	        throw new IllegalArgumentException("Invalid start port: " + port);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
	}

	public static String getErrorMessage(int errorNum)
    {
        switch (errorNum)
        {
            case VALUE_GOOD:
                return "OK";
            case VALUE_MOVED: // default homepage
                return "Moved Permanently";
            case VALUE_PROXY: // missing
                return "Use Proxy";
            case VALUE_PROXY2: // missing
                return "Special Proxy";
			case VALUE_BAD_REQUEST:
				return "Bad Request";
			case VALUE_FORBIDDEN:
				return "Forbidden";
            case VALUE_NOTFOUND: // deleted
                return "Not Found";
            case VALUE_BADMETHOD: // unused
                return "Method Not Allowed";
            case VALUE_INTERNAL_SERVER_ERROR: // handle read
                return "Internal Server Error";
            case VALUE_NOT_IMPLEMENTED: // POST
                return "Not Implemented";
            default:
                return "Unknown";
        }
    }

	static class ContentDisposition {
		public String type;
		public String name;
		public String filename;
	}

	static ContentDisposition parseContentDisposition(String contentDispositionString) {
		ContentDisposition obj = new ContentDisposition();

		String[] parts = contentDispositionString.split(";");
		if (parts.length > 0) {
			obj.type = parts[0];
		}
		for (int i = 1; i < parts.length; i++) {
			String[] keyValue = parts[i].split("=", 2);
			if (keyValue.length == 2) {
				String key = keyValue[0].trim();
				String value = keyValue[1].trim();

				if (value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}

				if (key.equals("name")) {
					obj.name = value;
				} else if (key.equals("filename")) {
					obj.filename = value;
				}
			}
		}

		return obj;
	}
}

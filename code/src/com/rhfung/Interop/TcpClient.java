package com.rhfung.Interop;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Emulation of .NET TcpClient
 * @author Richard
 *
 */
public class TcpClient {
	private Socket m_socket;
	
	public TcpClient(Socket s)
	{
		m_socket = s;
		try
		{
			m_socket.setKeepAlive(true);
		}
		catch(Exception ex)
		{
		}
	}
	
	public OutputStream getOutputStream() throws IOException
	{
		return m_socket.getOutputStream();
	}
	
	public InputStream getInputStream() throws IOException
	{
		return m_socket.getInputStream();
	}
	
	public boolean isConnected()
	{
		return m_socket.isConnected() ;
	}
	
	public EndPoint getRemoteEndPoint()
	{
		return new  EndPoint( m_socket.getInetAddress(), m_socket.getPort());
	}
	
	/**
	 * Method not implemented, always returns true
	 * @return
	 */
	public boolean isDataAvailable()
	{
		return true;
//		try
//		{
//			return (m_socket.getInputStream().available() != 0);
//		}
//		catch(IOException ex)
//		{
//			return false;
//		}
	}
	
	public void close() throws IOException
	{
			m_socket.close();
	}

	public EndPoint getLocalEndPoint() {
		// TODO Auto-generated method stub
		return new EndPoint(m_socket.getLocalAddress(), m_socket.getLocalPort());
	}
}

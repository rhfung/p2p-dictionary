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

import java.net.InetAddress;

/**
 * Emulates a .NET EndPoint for P2P dictionary
 * @author Richard
 *
 */
public class EndPoint {
	private InetAddress m_Address;
	private int m_Port;
	
	public EndPoint(InetAddress addr, int port)
	{
		this.m_Address = addr;
		this.m_Port = port;
	}
	
	public InetAddress getAddress() {
		return m_Address;
	}
	public void setAddress(InetAddress m_Address) {
		this.m_Address = m_Address;
	}
	
	public int getPort() {
		return m_Port;
	}
	public void setPort(int m_Port) {
		this.m_Port = m_Port;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return m_Address.getHostAddress() + ":" + m_Port;
	}

	public boolean equals(Object obj) {
		if (obj instanceof EndPoint) {
			EndPoint obj2 = (EndPoint) obj;
			return obj2.m_Address.equals(this.m_Address) &&
					obj2.m_Port == this.m_Port;
		} else {
			return false;
		}
	}
}

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

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

class UIDGenerator {
	static int used_random_bits = 0;

	// NOTE: this should be done not at startup
	static {
		// to better randomize the seed, include all MAC addresses on the local
		// interfaces
		Enumeration<java.net.NetworkInterface> ifaces;
		try {
			ifaces = java.net.NetworkInterface.getNetworkInterfaces();
				
			while (ifaces.hasMoreElements()) {
				java.net.NetworkInterface face = ifaces.nextElement();
				if (face != null)
				{
					Enumeration<InetAddress> ips = face.getInetAddresses();
					if (ips !=null)
					while( ips.hasMoreElements())
					{
						used_random_bits +=ips.nextElement().hashCode();
					}
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence. 
	 * @return
	 */
	public static int GetNextInteger() {
		Random r = new Random((int) ((new Date()).getTime() + used_random_bits));
		int rnd = r.nextInt(Integer.MAX_VALUE);
		used_random_bits += rnd;
		return rnd;
	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence. 
	 * @param max_value
	 * @return
	 */
	public static int GetNextInteger(int max_value) {
		Random r = new Random((int) ((new Date()).getTime() + used_random_bits));
		int rnd = r.nextInt(max_value);
		used_random_bits += rnd;
		return rnd;
	}

}

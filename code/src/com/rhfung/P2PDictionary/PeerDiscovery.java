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

import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.apple.dnssd.*;

/**
 * Partial implementation only registers server. Does not unregister.
 * @author Richard
 *
 */
class PeerDiscovery {
	public static Hashtable<Integer, List<EndpointInfo>> DiscoveredPeers ;
	private static final String ZEROCONF_NAME = "_com-rhfung-peer._tcp";
	
	private volatile boolean killBitDiscovery = false;
	
	private volatile DNSSDRegistration reg = null;
	private volatile DNSSDService service = null;
	
	class AppResolveListener implements ResolveListener
	{

		@Override
		public void operationFailed(DNSSDService arg0, int arg1) {
			arg0.stop();
		}
		
		@Override
		public void serviceResolved(DNSSDService resolver, int flags, int ifIndex,
				String fullName, String hostName, int post, TXTRecord txtRecord) {
			// serviceResolved(DNSSDService resolver, int flags, int ifIndex, java.lang.String fullName, java.lang.String hostName, int port, TXTRecord txtRecord)
	          
			// arg3 fullname
			// arg4 hostname
			// arg5 port
			
			if (txtRecord.contains("uid"))
			{
				String uid = txtRecord.getValueAsString("uid");
				int uidInt = Integer.parseInt(uid);
				synchronized (DiscoveredPeers) {
					
				
					if (!DiscoveredPeers.containsKey(uidInt))
					{
						DiscoveredPeers.put(uidInt, new Vector<EndpointInfo>(0));
					}
				}
				
				try {
					synchronized (DiscoveredPeers.get(uidInt)) {
						if (DiscoveredPeers.get(uidInt).size() < 10)
						{
							DiscoveredPeers.get(uidInt).add(new EndpointInfo(uidInt, java.net.InetAddress.getByName(hostName), post ));
						}
						else
						{
							// ignore or stop??
						}
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					
				}
			}
			
			if (killBitDiscovery)
			{
				resolver.stop();
				return;
			}
			else if ((DNSSD.MORE_COMING & flags) != DNSSD.MORE_COMING)
			{
				resolver.stop();
			}
			
		}
	}
	
	static
	{
		DiscoveredPeers = new Hashtable<Integer, List<EndpointInfo>>();
	}
	
	/**
	 * Start browsing Apple Bonjour for the P2P dictionary.
	 */
	public void BrowseServices()
	{
		if (service !=null)
			service.stop();
		
		try {
			service = DNSSD.browse(ZEROCONF_NAME, new BrowseListener() {
				
				@Override
				public void operationFailed(DNSSDService arg0, int arg1) {
					if (killBitDiscovery)
						arg0.stop();

				}
				
				@Override
				public void serviceLost(DNSSDService resolver, int flags, int arg2, String arg3,
						String arg4, String arg5) {
					if (killBitDiscovery)
						resolver.stop();
				

				}
				
				@Override
				public void serviceFound(DNSSDService arg0, int arg1, int arg2,
						String arg3, String arg4, String arg5) {
					//public void serviceFound(DNSSDService browser,
//                    int flags,
//                    int ifIndex,
//                    java.lang.String serviceName,
//                    java.lang.String regType,
//                    java.lang.String domain)
					if (killBitDiscovery)
					{
						arg0.stop();
						return;
					}
					
					try {
						DNSSD.resolve(0, arg2, arg3, arg4, arg5, new AppResolveListener());
					} catch (DNSSDException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} catch (DNSSDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds record to Bonjour
	 * @param dict
	 */
	public void RegisterServer(P2PDictionary dict)
    {
		TXTRecord record = new TXTRecord();
		record.set("uid", Integer.toString( dict.getLocalID()));

		if (reg != null)
		{
			reg.stop();
		}
		
		try {
			reg = DNSSD.register(0, 0, "com.rhfung.P2PDictionary " + dict.getDescription(), ZEROCONF_NAME, null, null, dict.getLocalEndPoint().getPort(), record, new RegisterListener() {
				
				@Override
				public void operationFailed(DNSSDService arg0, int arg1) {
					if (killBitDiscovery)
						arg0.stop();
				}
				
				@Override
				public void serviceRegistered(DNSSDRegistration arg0, int arg1,
						String arg2, String arg3, String arg4) {
					if (killBitDiscovery)
						arg0.stop();
					
				}
			} );
			
		} catch (DNSSDException e) {
			
			e.printStackTrace();
		}
		catch(RuntimeException ex)
		{
			ex.printStackTrace();
		}
    }
	
	/**
	 * Stop registration and discovery service.
	 */
	 public void UnregisterServer()
	 {
		 killBitDiscovery = true;
		 
		 if (reg != null)
		 {
			 reg.stop();
			 reg=null;
		 }
		 
		if (service !=null)
		{
			service.stop();
			service =null;
		}
	 }

	 
}

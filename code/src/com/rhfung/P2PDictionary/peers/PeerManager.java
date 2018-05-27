package com.rhfung.P2PDictionary.peers;

//P2PDictionary
//Copyright (C) 2016, Richard H Fung (www.richardhfung.com)
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

import com.rhfung.Interop.EndPoint;
import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.logging.LogInstructions;

import java.util.Hashtable;
import java.util.List;


/**
 * Partial implementation only registers server. Does not unregister.
 * @author Richard
 *
 */
public class PeerManager {
	public static final String ZEROCONF_NAME = "_com-rhfung-peer._tcp.local.";

	private static Hashtable<Integer, EndpointList> m_discoveredPeers;
	private LogInstructions m_log;

	public synchronized static Hashtable<Integer, EndpointList> getDiscoveredPeers() {
		if (m_discoveredPeers == null) {
			m_discoveredPeers = new Hashtable<Integer, EndpointList>();
		}
		return  m_discoveredPeers;
	}

	private PeerInterface m_peerInterface = null;

	public PeerManager(LogInstructions debugBuffer, PeerInterface discovery) {
		m_peerInterface = discovery;
		m_log = debugBuffer;
		m_peerInterface.ConfigureLogging(m_log);
	}
	
	/**
	 * Start browsing Apple Bonjour for the P2P dictionary.
	 */
	public void BrowseServices()
	{
		if (m_log != null) {
			m_log.Log(LogInstructions.INFO, "Browsing for services", true);
		}

		m_peerInterface.BrowseServices();
	}
	
	/**
	 * Adds record to Bonjour
	 * @param dict
	 */
	public void RegisterServer(P2PDictionary dict)
    {
		if (m_log != null) {
			m_log.Log(LogInstructions.INFO, "Registering server " + dict.getLocalID() + " with peer discovery", true);
		}
		m_peerInterface.RegisterServer(dict);
    }
	
	/**
	 * Stop registration and discovery service.
	 */
	 public void UnregisterServer()
	 {
		 if (m_log != null) {
			 m_log.Log(LogInstructions.INFO, "Unregistering server", true);
		 }

		 m_peerInterface.UnregisterServer();
	 }

	public static void updateEntry(int peerId, EndPoint newInfo) {

		// update matched entry
		if (getDiscoveredPeers().containsKey(peerId)) {
			EndpointInfo infoEntry = new EndpointInfo(peerId, newInfo.getAddress(), newInfo.getPort());
			EndpointList endpoints = getDiscoveredPeers().get(peerId);
			if (!endpoints.contains(infoEntry)) {
				endpoints.add(infoEntry);
			}
		}

		// de-dup the IP address and port
		for (Integer keyId : getDiscoveredPeers().keySet()) {
			if (peerId != keyId) {
				EndpointList list = getDiscoveredPeers().get(keyId);
				for (int i = list.size(); i >= 0 && i < list.size(); i--) {
					EndpointInfo info = list.get(i);
					if (info.Address.equals(newInfo.getAddress()) &&
							info.Port == newInfo.getPort()) {
						list.remove(info);
					}
				}
			}
		}
	}
}

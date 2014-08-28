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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;

import com.rhfung.Interop.EndPointMetadata;


class WeakDataServer implements IMessageController
{
	private WeakReference<P2PDictionary> m_target;
	
    public WeakDataServer(P2PDictionary target) 
    { 
    	m_target = new WeakReference<P2PDictionary>(target);
    }

	@Override
	public String getDescription() {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.getDescription();
		else
			return "";
	}

	@Override
	public List<EndPointMetadata> getActiveEndPoints() {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.getActiveEndPoints();
		else
			return new Vector<EndPointMetadata>();
	}

	@Override
	public List<EndpointInfo> getAllEndPoints() {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.getAllEndPoints();
		else
			return new Vector<EndpointInfo>();
	}

	@Override
	public int onBroadcastToWire(SendBroadcastMemory message) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.onBroadcastToWire(message);
		else
		return 0;
	}

	@Override
	public int onPullFromPeer(DataHeader header) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.onPullFromPeer(header);
		else
			return 0;
	}

	@Override
	public int onPullFromPeer(List<DataHeader> header) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.onPullFromPeer(header);
		else
			return 0;
	}

	@Override
	public int onSendToPeer(SendMemoryToPeer message) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.onSendToPeer(message);
		else
			return 0;
	}

	@Override
	public int onSendToPeer(List<SendMemoryToPeer> message) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.onSendToPeer(message);
		else
			return 0;
	}

	@Override
	public boolean isConnected(int uniqueID) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.isConnected(uniqueID);
		else
			return false;
	}

	@Override
	public void onConnected(DataConnection conn) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			d.onConnected(conn);
		
	}

	@Override
	public void onDisconnected(DataConnection conn) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			d.onDisconnected(conn);		
	}

	@Override
	public void onNotified(NotificationEventArgs args) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			d.onNotified(args);
		
	}

	@Override
	public void onSubscriptionChanged(SubscriptionEventArgs args) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			d.onSubscriptionChanged(args);
		
	}

	@Override
	public String getDefaultKey() {
		P2PDictionary d= m_target.get();
		if (d!=null)
					return d.getDefaultKey();
		else
			return "";
	}

	@Override
	public boolean containsKey(String key) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.containsKey(key);
		else
			return false;
	}

	@Override
	public String getFullKey(String userKey) {
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.getFullKey(userKey);
		else
			return userKey;
	}

	@Override
	public Object put(String key, Object value) {
		// TODO Auto-generated method stub
		P2PDictionary d= m_target.get();
		if (d!=null)
			return d.put(key, value);
		else
			return null;
	}



}

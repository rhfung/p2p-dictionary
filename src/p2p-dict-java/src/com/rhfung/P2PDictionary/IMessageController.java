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

import java.util.List;

import com.rhfung.Interop.EndPointMetadata;

interface IMessageController {
    /// <summary>
    /// User-friendly description of the working dictionary for web access
    /// </summary>
    String getDescription();
    

    /// <summary>
    /// Item2: UID
    /// Item3: false - server, true - client
    /// </summary>
    List<EndPointMetadata> getActiveEndPoints();

    List<EndpointInfo> getAllEndPoints();

    /// <summary>
    /// Broadcasts a message to all peers.
    /// </summary>
    /// <param name="message">Message is sent to every peer except those listed in Senders.</param>
    int onBroadcastToWire(SendBroadcastMemory message);

    /// <summary>
    /// Requests data from a specific sender(s).
    /// </summary>
    /// <param name="header"></param>
    int onPullFromPeer(DataHeader header);

    /// <summary>
    /// Requests data from a specific sender(s).
    /// </summary>
    /// <param name="header"></param>
    int onPullFromPeer(List<DataHeader> header);

    /// <summary>
    /// Sends a message to a specific sender(s).
    /// </summary>
    /// <param name="message">senders list is used to target a peer</param>
    int onSendToPeer(SendMemoryToPeer message);

    /// <summary>
    /// Sends a message to a specific sender(s).
    /// </summary>
    /// <param name="message">senders list is used to target a peer</param>
    int onSendToPeer(List<SendMemoryToPeer> message);

    boolean isConnected(int uniqueID);
    
    String getDefaultKey();
    
    boolean containsKey(String key);
    
    String getFullKey(String userKey);
    
    Object put(String key, Object value);

    // wire through events

    void onConnected(DataConnection conn);
    void onDisconnected(DataConnection conn);
    void onNotified(NotificationEventArgs args);
    void onSubscriptionChanged(SubscriptionEventArgs args);

}

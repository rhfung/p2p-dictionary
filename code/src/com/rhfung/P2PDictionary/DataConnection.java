package com.rhfung.P2PDictionary;

//  P2PDictionary
//  Copyright (C) 2013, Richard H Fung (www.richardhfung.com)
// 
//  Permission is hereby granted to any person obtaining a copy of this software 
//	and associated documentation files (the "Software"), to deal in the Software 
//	for the sole purposes of PERSONAL USE. This software cannot be used in 
//	products where commercial interests exist (i.e., license, profit from, or
//	otherwise seek monetary value). The person DOES NOT HAVE the right to
//	redistribute, copy, modify, merge, publish, sublicense, or sell this Software
//	without explicit prior permission from the author, Richard H Fung.
//	
//	The above copyright notice and this permission notice shall be included 
//	in all copies or substantial portions of the Software.
//	
//	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
//	THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//	THE SOFTWARE.


// Reader writer lock
// http://www.bluebytesoftware.com/blog/PermaLink,guid,c4ea3d6d-190a-48f8-a677-44a438d8386b.aspx

// limitation in revision #
// once it rolls negative, the whole thing falls apart

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;

import com.rhfung.P2PDictionary.subscription.Subscription;
import com.rhfung.logging.LogInstructions;
import org.apache.commons.fileupload.MultipartStream;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.rhfung.Interop.EndPointMetadata;
import com.rhfung.Interop.ListInt;
import com.rhfung.Interop.MemoryStream;
import com.rhfung.Interop.NotImplementedException;
import com.rhfung.Interop.StreamWriter;
import com.rhfung.Interop.TcpClient;
import com.rhfung.P2PDictionary.Encodings.ValueType;


class DataConnection
    {
		public static final String ROOT_NAMESPACE = "/";     
		public static final String DATA_NAMESPACE = "/data";
        public static final String CLOSE_MESSAGE = "/close"; 
        public static final String SUBSCRIPTIONS_NS = "/subscriptions";
        public static final String CONNECTIONS_NS = "/connections";
        public static final String PROXY_PREFIX = "/proxy";
        public static final String BONJOUR_NS = "/network";
        public static final String ADDENTRY_NS_API = "/data";
        public static final String ADDENTRY_NS_MIME_API = "/data?storeas=mime";
        public static final String ADDENTRY_NS_BYTES_API = "/data?storeas=bytes";
        public static final int PROXYPREFIX_REMOVE = 6;
        public static final String ISO8859 = "ISO-8859-1";
        
        // verbs
        public static final String GET = "GET";        // does not carry payload; payload in response
        public static final String HEAD = "HEAD";      // does not carry payload; no payload in response
        public static final String PUT = "PUT";        // PUT creates or overwrites resource; has paylod; no response
        public static final String DELETE = "DELETE";  // removes a resource, no payload, no response
        public static final String PUSH = "PUSH";      // not in HTTP: this announces changes, no payload, no response
        public static final String POST = "POST";	   // handles adding resources from a REST API, has payload, payload in response

        // response codes
        public static final String RESPONSECODE_GOOD = "200";
        public static final String RESPONSECODE_PROXY = "305";
        public static final String RESPONSECODE_PROXY2 = "307";
        public static final String RESPONSECODE_DELETED = "404";
        public static final int RESPONSEVALUE_NOTFOUND = 404;

        static final String NEWLINE = "\r\n";
        static final String HEADER_SPECIAL = "P2P-Dictionary";
        static final String HEADER_LOCATION = "Content-Location";
        static final int BACKLOG = 1024;
        
        private static final String RESOURCE_INDEX = "/index.html";
        private static final String RESOURCE_ERROR = "/error.html";
        
        

        private int local_uid = 0;
        private int remote_uid = 0;

        private volatile boolean killBit = false;

        private int adaptive_conflict_bound = P2PDictionary.MAX_RANDOM_NUMBER;

        private Thread runThread;

        public LogInstructions debugBuffer;

        private Map<String, DataEntry> data;
        private ReadWriteLock dataLock;
        private TcpClient client;

        private Queue<MemoryStream> sendBuffer;
        private Map<String, DataHeader> receiveEntries;
        private Map<String, SendMemory> sendEntries;
        private Queue<String> sendQueue;


        private volatile ConnectionState state;
        private Subscription keysToListen;
        private ConnectionType _connectionType ;
        // messages
        private IMessageController controller;

        private enum ConnectionState
        {
            NewConnection,
            WebClientConnected,
            PeerNodeConnected,
            Closing,
            Closed
        }

  
        
        /// <summary>
        /// 
        /// </summary>
        /// <param name="type"></param>
        /// <param name="loopThread"></param>
        /// <param name="localUID"></param>
        /// <param name="sharedData"></param>
        /// <param name="sharedDataLock">all operations that modify sharedData (add and remove) should lock using this object</param>
        /// <param name="controller"></param>
        /// <param name="keysToListen"></param>
        /// <param name="debug"></param>
        public DataConnection(ConnectionType type, int localUID,
            Map<String, DataEntry> sharedData, ReadWriteLock sharedDataLock, IMessageController controller,
            Subscription keysToListen, LogInstructions debug)
        {
            this._connectionType = type;
            this.local_uid = localUID;
            this.data = sharedData;
            this.dataLock = sharedDataLock;
            this.state = ConnectionState.NewConnection;

            // send and receive queues
            this.sendBuffer = new LinkedList<MemoryStream>();
            this.sendEntries = new Hashtable<String, SendMemory>();
            this.sendQueue = new LinkedList<String>();
            this.receiveEntries = new Hashtable<String,DataHeader>();

            this.keysToListen = keysToListen;

            // delegate to send messages
            this.controller = controller;

            this.debugBuffer = debug;
        }
        
        public void setThread(Thread thread)
        {
        	runThread = thread;
        }

        public String toString()
        {
            return local_uid + " to " + remote_uid + " (" + getRemoteEndPoint().toString() + ")";
        }

        /// <summary>
        /// Closes the TCP connection as soon as possible.
        /// </summary>
        public void Close()
        {
            Close(false);
        }

        /// <summary>
        /// Closes the TCP connection as soon as possible.
        /// </summary>
        void Close(boolean disposing)
        {
            // TODO: need to create an asymmetric close handshake
            SendMemoryToPeer endMsg = new SendMemoryToPeer(CLOSE_MESSAGE, ListInt.createList( this.remote_uid));
            ResponseCode(endMsg.MemBuffer.createStreamWriter(), CLOSE_MESSAGE, GetListOfThisLocalID(), 0, 0, 200);
            SendToRemoteClient(endMsg);

            if (!disposing)
            {
                int counter = 0;
                // spin wait
                while (this.state != ConnectionState.Closed && counter < 10) // ~ 1 second
                {
                    try {
						Thread.sleep(P2PDictionary.SLEEP_WAIT_TO_CLOSE);
					} catch (InterruptedException e) {
						break;
					}
                    counter++;
                }

                if (runThread != null && runThread.isAlive()) {
                    WriteDebug("Halting connection thread");
                    Kill();
                }
            }
        }


        /**
         * Deprecated
         */
        public void Abort()
        {
            if (this.state != ConnectionState.Closed )
            {
                this.state = ConnectionState.Closing;
                this.killBit = true;
            }
            
            // spin wait
            while (this.state != ConnectionState.Closed)
            {
            	try
            	{
            		Thread.sleep(P2PDictionary.SLEEP_WAIT_TO_CLOSE);
            	}
            	catch(InterruptedException ex)
            	{
            		break;
            	}
            }
        }

        /**
         * Closes the TCP connection immediately and then terminates the thread.
         * @return true if another thread is interrupted; false if the current thread is invoking this method
         */
        public boolean Kill()
        {
            this.killBit = true;

            if (this.state != ConnectionState.Closed )
            {
                this.state = ConnectionState.Closing;
                this.killBit = true;
            }

            if (!Thread.currentThread().equals(runThread) && runThread.isAlive())
            {
            	// TODO: check to see if this is the correct behaviour
                runThread.interrupt();
                return true;
            }
            else
            {
                return false;
            }
        }

        public boolean isConnected()
        {
            
            return this.client != null && this.client.isConnected();
            
        }

        public ConnectionType isClientConnection()
        {
            
                return this._connectionType;
            
        }

        public int getLocalUID()
        {
                return this.local_uid;
        }

        public int getRemoteUID()
        {
            
                return this.remote_uid;
            
        }

        public com.rhfung.Interop.EndPoint getRemoteEndPoint()
        {
            if (this.client != null)

                return this.client.getRemoteEndPoint();
            else
                return null;
           
        }

        public boolean isWebClientConnected()
        {
            
                return state == ConnectionState.WebClientConnected || state == ConnectionState.NewConnection;
            
        }

        /// <summary>
        /// Creates a message with LocalUID as the sender list
        /// </summary>
        /// <param name="key">Any dictionary element in the data/* namespace, or the data dictionary itself.</param>
        /// <returns></returns>
        public SendMemoryToPeer CreateResponseMessage(String key)
        {
            return CreateResponseMessage(key, key, GetListOfThisLocalID(), null, null);
        }

        /// <summary>
        /// Creates a message with LocalUID as the sender list
        /// </summary>
        /// <param name="proxyKey">The resource name to return, prefixed with proxy/* for a proxy response.</param>
        /// <param name="key">Any dictionary element in the data/* namespace, or the data dictionary itself.</param>
        /// <returns></returns>
        public SendMemoryToPeer CreateResponseMessage(String key, String proxyKey, ListInt senderPath, ListInt  includeList, ListInt proxyResponsePath)
        {
            SendMemoryToPeer msg = new SendMemoryToPeer(proxyKey, includeList);

            if (key.equals( DATA_NAMESPACE))
            {
            	StreamWriter writer = new StreamWriter(msg.MemBuffer);
                ResponseDictionaryText(GET, writer, senderPath, proxyResponsePath, false);
            }
            else
            {
                DataEntry entry = P2PDictionary.getEntry( this.data, this.dataLock, key);
                if (!entry.subscribed)
                {
                    throw new NotImplementedException();
                }

                Response(GET, proxyKey, senderPath, proxyResponsePath, entry, msg.MemBuffer.createStreamWriter(), false);
            }

            return msg;
        }

        /// <summary>
        /// Adds a packet to the out-buffer of the current connection.
        /// Duplicate content is removed.
        /// </summary>
        /// <param name="msg"></param>
        public void SendToRemoteClient(SendMemory msg)
        {
            synchronized (sendEntries)
            {
            	sendEntries.put(msg.ContentLocation,  msg);
            }

            synchronized (sendQueue)
            {
                sendQueue.add(msg.ContentLocation);
            }
        }

        /// <summary>
        /// Adds a request to get data from the remote side.
        /// Call RemoveOldRequest() before this method.
        /// </summary>
        /// <param name="h"></param>
        public void AddRequest(DataHeader h)
        {
            synchronized (this.receiveEntries)
            {
                this.receiveEntries.put(h.key, h);
            }
            synchronized (this.sendQueue)
            {
                this.sendQueue.add(h.key);
            }
        }

        /// <summary>
        /// Add a request to get all data from the remote side.
        /// </summary>
        public void AddRequestDictionary()
        {
            synchronized (this.receiveEntries)
            {
                this.receiveEntries.put(DATA_NAMESPACE, new DataHeader(DATA_NAMESPACE, new ETag(0, 0),  this.local_uid)); //trigger a full update
            }
            synchronized (this.sendQueue)
            {
                this.sendQueue.add(DATA_NAMESPACE);
            }
        }

        /// <summary>
        /// Removes a previous request based on its contentLocation and old version requested.
        /// </summary>
        /// <param name="request"></param>
        /// <returns>true if the request is removed, false otherwise</returns>
        public boolean RemoveOldRequest(DataHeader request)
        {
            synchronized (this.receiveEntries)
            {
                
                if (receiveEntries.containsKey(request.key))
                {
                    DataHeader h = receiveEntries.get(request.key);
                    ETagCompare result = ETag.CompareETags(h.GetETag(), request.GetETag());
                    if (result == ETagCompare.SecondIsNewer || result == ETagCompare.Conflict)
                    {
                        // another version of the tag arrived, pull this request and have the new data requested
                        this.receiveEntries.remove(request.key);
                        return true;
                    }
                }
            }

            // this request is the newest
            return false;
        }


        public boolean HasRequest(String contentLocation)  
        {
            synchronized (this.receiveEntries)
            {
                return this.receiveEntries.containsKey(contentLocation);
            }
        }

        private void WriteDebug(String msg)
        {
            if (debugBuffer != null)
            {
                debugBuffer.Log(LogInstructions.INFO, msg, true);
            }
        }

        /// <summary>
        /// Thread's main function.
        /// </summary>
        /// <param name="data">TCP channel for communication. Bi-directional.</param>
        public void ReadLoop(TcpClient data)
        {
            this.client = data;

            WriteDebug(this.local_uid +  " Connection " + client.getLocalEndPoint().toString() + " -> " + client.getRemoteEndPoint().toString() + " " + this.runThread.getName());

            try
            {
            	
                InputStreamReader reader = new InputStreamReader (client.getInputStream(), Charset.forName(ISO8859));

                while (client.isConnected() && !killBit && (state != ConnectionState.Closing))
                { 
                    if (!HandleRead(reader))
                    	break;	
                }
            }
            catch (IOException ex)
            {
                // good bye
				
			} 
            

            this.state = ConnectionState.Closed;
            WriteDebug(this.local_uid + " Closed "+ this.runThread.getName());
            

            try
            {
                // only report P2P connections
                if (this.remote_uid != 0)
                    controller.onDisconnected(this);

                this.client.close();
            }
            catch (IOException ex)
            {
            }
            finally
            {
                this.client = null;
            }
        }

        /*
         * Splits a string into up to three parts:
         * first
         * first last
         * first middle middle last
         */
        private static String[] splitFrontEnd3(String input)
        {
        	int firstSpace = input.indexOf(" ");
        	int lastSpace = input.lastIndexOf(" ");
        	if (0 < firstSpace  && firstSpace < lastSpace)
        	{
        		// three-part string formed
        		return new String[] { input.substring(0, firstSpace), 
        				input.substring(firstSpace + 1, lastSpace),
        				input.substring(lastSpace + 1) };
        	}
        	else
        	{
        		if (firstSpace > 0)
        			return new String[] { input.substring(0, firstSpace), input.substring(firstSpace + 1) };
        		else
        			return new String[] { input };
        	}
        }
        
        private boolean HandleRead(InputStreamReader reader)
        {
        	String command ;
        	try
        	{
	            command = ReadLineFromBinary(reader);
	            if (command == null)
	                return false;
        	}
        	catch(IOException ex)
        	{
        		state =  ConnectionState.Closing;
        		return false;
        	}


            //String[] parts = command.split(" ", 3);
        	String[] parts = splitFrontEnd3(command); 
            
            // pull using a GET or HEAD command

            if (debugBuffer != null)
                debugBuffer.Log(LogInstructions.DEBUG, command, true);

            if (parts[0].equals( GET) || parts[0].equals(HEAD))
            {
                Hashtable<String, String> headers = ReadHeaders(reader);
                String contentLocation = URLUtils.URLDecode(parts[1]);
                HandleReadGetOrHead(reader, headers, parts[0], contentLocation);
            }
            else if (parts[0].equals( PUT) ||
            		parts[0].equals( DELETE) ||
            		parts[0].equals( POST) || 
            		parts[0].equals( PUSH))
            {
            	Hashtable<String, String> headers = ReadHeaders(reader);
                String contentLocation = URLUtils.URLDecode( parts[1]);
                HandleReadOne(parts[0], contentLocation, reader, headers);
            }
            // handle server 
            else if (parts[0].equals( "HTTP/1.0") || parts[0].equals("HTTP/1.1"))
            {
                String responseCode = parts[1];// 200, 305, 307, 404

                Hashtable<String, String> headers = ReadHeaders(reader);

                String verb;
                if (responseCode.equals(RESPONSECODE_PROXY))
                {
                    verb = RESPONSECODE_PROXY;
                }
                else if (responseCode.equals(RESPONSECODE_PROXY2))
                {
                    verb = RESPONSECODE_PROXY2;
                }
                else// assume RESPONSECODE_GOOD
                {
                    // detect a response to a GET or HEAD request
                    if (headers.containsKey("Response-To"))
                    {
                        verb = headers.get("Response-To");

                        if (verb.equals(GET))
                        {
                            if (responseCode.equals(RESPONSECODE_DELETED)) {
                                verb = DELETE;
                            } else {
                                verb = PUT;
                            }
                        }
                        else if (verb.equals(HEAD))
                        {
                            verb = PUSH;  // HEAD carries no payload
                        }
                        else
                        {
                        	// TODO: changed C# from NotSupported to NotImplemented
                            throw new NotImplementedException("Unsupported verb in Response-To");
                        }
                    }
                    else
                    {
                        throw new NotImplementedException("GET or HEAD required in Response-To");
                    }
                }

                String contentLocation = headers.get(HEADER_LOCATION);

                HandleReadOne(verb, contentLocation, reader, headers);

                
            }



            else // not a GET command or a HTTP response that server can understand
            {
                WriteDebug("Unknown request - emptying buffer");

                // finish reading the command, read until a blank line is reached
                try
                {
	                do
	                {
	                    command = ReadLineFromBinary(reader);
	                } while (command.length() > 0);
                }
                catch(IOException ex)
                {
                }

                MemoryStream bufferedOutput = new MemoryStream();
                WriteErrorNotFound(bufferedOutput.createStreamWriter(), "GET", parts[1], 500);
                synchronized (sendBuffer)
                {
                    sendBuffer.add(bufferedOutput);
                }
            }
            
            return true;
        }
        
        private boolean DetectBrowser(Hashtable<String, String> headers)
        {
            boolean browserRequest = false;            // detect browser
        	browserRequest = !headers.containsKey(HEADER_SPECIAL);

            if (this.state == ConnectionState.NewConnection)
            {

	            // assign remote UID
	            if (headers.containsKey(HEADER_SPECIAL))
	            {
	                int remoteID = Integer.parseInt(headers.get(HEADER_SPECIAL));
	
	                // stop duplicate connections
	                if (controller.isConnected(remoteID) || remoteID == this.local_uid)
	                {
	                	WriteDebug("Detected loopback connection");
	                	
	                    //force close
	                    this.remote_uid = remoteID;
	                    this.state = ConnectionState.Closing;
	                    browserRequest = true;
	                }
	                else
	                {
	                	WriteDebug("Hello " + remoteID);
	                	
	                    // finish the connection
	                    this.remote_uid = remoteID;
	                    this.state = ConnectionState.PeerNodeConnected;
	
	                    controller.onConnected(this);
	                }
	            }
	            else
	            {
	            	WriteDebug("Hello web browser");
	                this.state = ConnectionState.WebClientConnected;
	            }
            }
            
            return browserRequest;
        }
        
        
        //http://www.coderanch.com/t/383310/java/java/parse-url-query-string-parameter
        private static Map<String, String> getQueryMap(String query)  
        {  
            String[] params = query.split("&"); 
            Map<String, String> map = new HashMap<String, String>();  
            for (String param : params)  
            {  
            	String[] p = param.split("=",2);
            	if (p.length == 2)
            	{
	                String name = p[0];  
	                String value = p[1];  
	                map.put(name, value);
            	}
            }  
            return map;  
        }  
        
        private void HandleReadGetOrHead(InputStreamReader reader, Hashtable<String, String> headers, String verb, String resource)
        {

            MemoryStream memBuffer = new MemoryStream();
        	
            
            // this part is very simple
            // just look up the data that the other side requested and give the data

            // detect for web browser
            //bytesRead += command.Length + NetworkDelay.CountHeaders(headers);


//#if SIMULATION
//                            // 8 - 81ms delay in N.America      http://ipnetwork.bgtmo.ip.att.net/pws/network_delay.html
//                            // 100 Mb/s link
//                            Thread.Sleep(NetworkDelay.GetLatency(8, 81, 13107200, bytesRead));
//#endif

                boolean browserRequest = DetectBrowser(headers);
                

                
//                {
//    				String[] res = resource.split("\\?", 2);
//    				resource = res[0];
//    				String query = res.length > 1 ? res[1] : null;
//    				
//    				
//    				if (query != null)
//    				{
//    					String[] q = query.split("#", 2);
//    					
//    					 Map<String,String> queries= getQueryMap(q[0]);
//    					 if (queries.containsKey("format") )
//    					 {
//    						 retJson = queries.get("format").equals("json");
//    					 }
//    				}
//    				
//
//                }

            // see which resource is being accessed
            // latter half of condition is only for web browsers
            if (resource.equals( DATA_NAMESPACE) || resource.equals(DATA_NAMESPACE + "/"))
            {
            	boolean retJson = false;
            	if (headers.containsKey("Accept"))
    				retJson = headers.get("Accept").contains("application/json");
            	
                // whole dictionary
            	if (retJson)
            		ResponseDictionaryJson(verb, memBuffer.createStreamWriter(), GetListOfThisLocalID(), null, browserRequest);
            	else
            		ResponseDictionaryText(verb, memBuffer.createStreamWriter(), GetListOfThisLocalID(), null, browserRequest);
            }
            else if (resource.equals( ROOT_NAMESPACE))
            {
                ResponseIndex(verb, memBuffer.createStreamWriter(), browserRequest);
            }
            else if (resource.equals( CLOSE_MESSAGE) || resource.equals(CLOSE_MESSAGE + "/"))
            {
                // don't know what to do here
                WriteErrorNotFound(memBuffer.createStreamWriter(), verb, CLOSE_MESSAGE, 200);
            }
            else if (resource.equals( SUBSCRIPTIONS_NS ) || resource.equals(SUBSCRIPTIONS_NS + "/"))
            {

                ResponseSubscriptions(verb, memBuffer.createStreamWriter(), browserRequest);
            }
            else if (resource.equals( CONNECTIONS_NS) || resource.equals(CONNECTIONS_NS + "/"))
            {

                ResponseConnections(verb, memBuffer.createStreamWriter(), browserRequest);
            }
            else if (resource.equals(BONJOUR_NS) || resource.equals(BONJOUR_NS + "/"))
            {
                ResponseBonjour(verb, memBuffer.createStreamWriter(), browserRequest);
            }
            else if (this.data.containsKey(resource))
            {
                // handles current and expired data
                DataEntry entry = P2PDictionary.getEntry(this.data, this.dataLock, resource); //this.data[parts[1]];
                synchronized (entry)
                {
                    if (entry.subscribed && !DataMissing.isSingleton(entry.value))
                    {
                        // give the caller the data
                        Response(verb, resource, GetListOfThisLocalID(), null, entry, memBuffer.createStreamWriter(), browserRequest);
                    }
                    else
                    {
                        // tell the caller that a proxy must be used
                        ResponseCode(memBuffer.createStreamWriter(), resource, GetListOfThisLocalID(), entry.lastOwnerID, entry.lastOwnerRevision, 305);
                    }
                }
            }
            else if (resource.startsWith(PROXY_PREFIX + "/"))
            {
                throw new NotImplementedException();
            }
            else
            {
                // anything else
                WriteErrorNotFound(memBuffer.createStreamWriter(), verb, resource, 404, GetListOfThisLocalID());
            }

            // spit everything out of the writer
            
            synchronized (sendBuffer)
            {
                sendBuffer.add(memBuffer);
            }
        }

        private void HandleReadPut(String contentLocation, String contentType, byte[] readData, String eTag, ListInt senders, ListInt responsePath)
        {
            // process the packet
                if (contentLocation.equals( DATA_NAMESPACE))
                {
                    List<DataHeader> missingData = new Vector<DataHeader>();
                    List<SendMemoryToPeer> sendBack = new Vector<SendMemoryToPeer>();
                    ReadDictionaryTextFile(readData, new ListInt(senders), missingData, sendBack);

                    // and then update my copy of the dictionary 
                    controller.onPullFromPeer(missingData);

                    // and update the sender's dictionary
                    controller.onSendToPeer(sendBack);
                }
                else
                {
                    if (contentLocation.startsWith(PROXY_PREFIX + "/"))
                    {
                        // this is a pushed message from a proxy request
                        // so I should subscribe to the key

                        contentLocation = contentLocation.substring(PROXYPREFIX_REMOVE);
                        if (!keysToListen.isSubscribed(contentLocation))
                        {
                            keysToListen.AddSubscription(contentLocation, SubscriptionInitiator.AutoProxyKey);
                        }

                    }

                    ResponseAction status = ReadData(contentLocation, eTag, contentType, new ListInt(senders), readData);

                    // data propagation for following a proxy
                    if (responsePath != null)
                    {
                        ListInt followPath = new ListInt(responsePath);
                        followPath.remove(this.remote_uid);

                        // send data along the path
                        senders.add(this.local_uid);
                        SendMemoryToPeer sendMsg = CreateResponseMessage(contentLocation, PROXY_PREFIX + contentLocation, senders, followPath, responsePath);
                        controller.onSendToPeer(sendMsg);
                    }

                    if (status == ResponseAction.ForwardToAll)
                    {
                        //conflict happened in data somewhere
                        // return new data to sender
                        senders.clear();
                    }

                    if (status != ResponseAction.DoNotForward)
                    {
                        // add my sender to the packet
                        senders.add(this.local_uid);

                        // add to wire to send out
                        SendBroadcastMemory sendMsg = new SendBroadcastMemory(contentLocation, senders);
                        
                        DataEntry get = P2PDictionary.getEntry(this.data, this.dataLock, contentLocation);

                        WriteMethodPush(contentLocation, senders, responsePath, 0, get.getMime(), get.GetETag(), get.isDeleted(), false, sendMsg.MemBuffer.createStreamWriter());
                        //Response(verb, contentLocation, senders, this.data[contentLocation], sendMsg.MemBuffer, false);
                        controller.onBroadcastToWire(sendMsg);
                    }

                }
        }

        private void HandleReadPush(String contentLocation, String contentType, String eTag, ListInt senders, int lastSender, ListInt responsePath)
        {
            ETag tag = ReadETag(eTag);

            if (contentLocation.equals( DATA_NAMESPACE))
            {
                //// tell others that a new dictionary entered
                //// add to wire to send out
                //SendMemory sendMsg = new SendMemory(senders);
                //ResponseDictionary(verb, sendMsg.MemBuffer, senders, false);
                //controller.BroadcastToWire(sendMsg);

                // don't forward message because the GET method call will do it

                // let me update my model first by
                // requesting to pull data from the other side
                // before sending out a HEAD
                DataHeader hdr = new DataHeader(contentLocation, tag, lastSender);
                controller.onPullFromPeer(hdr);

            }
            else
            {
                if (contentLocation.startsWith(PROXY_PREFIX + "/"))
                {
                    // this is a pushed message from a proxy request
                    // so I should subscribe to the key

                    contentLocation = contentLocation.substring(PROXYPREFIX_REMOVE);
                    throw new NotImplementedException();
                }


                ResponseInstruction instr = ReadDataStub(contentLocation, contentType, eTag, new ListInt(senders));

                if (instr.action == ResponseAction.ForwardToAll)
                {
                    senders.clear();
                }

                if (instr.action != ResponseAction.DoNotForward)
                {
                    // forward a HEAD message (because we didn't do it when we got a 200/HEAD notification)
                    DataEntry get = P2PDictionary.getEntry(this.data, this.dataLock, contentLocation);

                    senders.add(this.local_uid);
                    SendBroadcastMemory sendMsg = new SendBroadcastMemory(contentLocation, senders);
                    WriteMethodPush(contentLocation, senders, responsePath, 0, get.getMime(), get.GetETag(), get.isDeleted(), false, sendMsg.MemBuffer.createStreamWriter());
                    controller.onBroadcastToWire(sendMsg);
                }

                if (instr.getEntryFromSender != null)
                {
                    // and get data from the caller
                    controller.onPullFromPeer(instr.getEntryFromSender);
                }


                if (instr.addEntryToSender != null)
                {
                    // send any updates to the peer
                    controller.onSendToPeer(instr.addEntryToSender);
                }
            }
        }

        private static String getBoundaryFromContentType(String contentType)
        {
        	final String BOUNDARY = "boundary=";
        	int startIndex = contentType.indexOf(BOUNDARY);
        	if (startIndex >= 0)
        		return contentType.substring(startIndex + BOUNDARY.length());
        	else
        		return null;
        }
        
        private void HandleReadPost(String contentLocation, String contentType, String accepts, ListInt senders, byte[] payload)
        {
            if (contentLocation.equals(ADDENTRY_NS_API) || 
            		contentLocation.equals(ADDENTRY_NS_BYTES_API)||
            		contentLocation.equals(ADDENTRY_NS_MIME_API))
            {
            	boolean sendMime = contentLocation.equals(ADDENTRY_NS_MIME_API);
            	
            	String boundary = getBoundaryFromContentType(contentType);
            	if (boundary == null)
            	{
            		WriteDebug("Cannot identify boundary from POST");
            		return;
            	}
            	
            	String filename = "content";
            	try {
            		
            		
            		MultipartStream multipartStream = new MultipartStream(new ByteArrayInputStream(payload), boundary.getBytes());
            	     boolean nextPart = multipartStream.skipPreamble();
            	     
            	     while(nextPart) {
            	       
            	    	String header = multipartStream.readHeaders();
            	    	Hashtable<String, String> hdr = ReadHeaders(new InputStreamReader(new ByteArrayInputStream(header.getBytes())) );
            	    	String val = hdr.get("Content-Disposition");
            	    	
            	    	if (val !=null)
            	    	{
            	    		final String FILENAME = "filename=\"";
            	    		int filenameIdx= val.indexOf(FILENAME);
            	    		filename = val.substring( filenameIdx + FILENAME.length(), val.indexOf("\"", filenameIdx + FILENAME.length()));
            	    		
            	    	}
            	    	
            	       // process headers
            	       // create some output stream
            	       ByteArrayOutputStream output = new ByteArrayOutputStream();
            	       multipartStream.readBodyData(output);
            	       if (sendMime)
            	       {
            	    	   String valType = hdr.get("Content-Type");
            	    	   controller.put(filename, new MIMEByteObject(valType, output.toByteArray()));
            	       }
            	    	   
            	       else
            	       {
            	    	   controller.put(filename, output.toByteArray());
            	       }
            	       nextPart = multipartStream.readBoundary();
            	     }

            	     // only reply back with ONE file uploaded
                 	MemoryStream bufferedOutput = new MemoryStream();
                	String key = controller.getFullKey(filename);
                	dataLock.readLock().lock();
                	try
                	{
                		DataEntry entry = data.get(key);
                		WriteResponseInfo(bufferedOutput.createStreamWriter(),  ADDENTRY_NS_API, entry);
                		synchronized (sendBuffer)
                        {
                            sendBuffer.add(bufferedOutput);
                        }
                	}
                	finally
                	{
                		dataLock.readLock().unlock();
                	}

                	
            	} catch(Exception e) {
            		MemoryStream bufferedOutput = new MemoryStream();
                    WriteErrorNotFound(bufferedOutput.createStreamWriter(), "GET", contentLocation, 500);
                    synchronized (sendBuffer)
                    {
                        sendBuffer.add(bufferedOutput);
                    }
        	   }
            	
            	
            	
            }
            else
            {
            	MemoryStream bufferedOutput = new MemoryStream();
                WriteErrorNotFound(bufferedOutput.createStreamWriter(), "GET", contentLocation, 501);
                synchronized (sendBuffer)
                {
                    sendBuffer.add(bufferedOutput);
                }
            }
        }

        
        private void HandleReadDelete(String contentLocation,String eTag, ListInt senders, ListInt responsePath )
        {
            // handle proxy messages
            if (contentLocation.startsWith(PROXY_PREFIX + "/"))
            {
                contentLocation = contentLocation.substring(PROXYPREFIX_REMOVE);
                if (!keysToListen.isSubscribed(contentLocation))
                {
                    keysToListen.AddSubscription(contentLocation, SubscriptionInitiator.AutoProxyKey);
                }
            }

            // read
            ResponseAction status = ReadDelete(contentLocation, eTag, new ListInt(senders));

            if (status == ResponseAction.ForwardToAll)
            {
                //conflict happened in data somewhere
                // return new data to sender
                senders.clear();
            }

            if (status != ResponseAction.DoNotForward)
            {
                // send a notification of deleted content immediately
                DataEntry entry = P2PDictionary.getEntry(this.data, this.dataLock, contentLocation);

                // add to wire to send out
                senders.add(this.local_uid);// add my sender to the packet
                SendBroadcastMemory sendMsg = new SendBroadcastMemory(entry.key, senders);
                WriteMethodDeleted(sendMsg.MemBuffer.createStreamWriter(), contentLocation, senders, responsePath, entry.lastOwnerID, entry.lastOwnerRevision);
                controller.onBroadcastToWire(sendMsg);
            }

            if (responsePath != null)
            {
                // well, i still have to send out this message because there is a path requested to follow
                DataEntry entry = P2PDictionary.getEntry(this.data, this.dataLock, contentLocation);

                SendMemoryToPeer sendMsg = new SendMemoryToPeer(entry.key, responsePath);
                senders.add(this.local_uid);// add my sender to the packet
                WriteMethodDeleted(sendMsg.MemBuffer.createStreamWriter(), PROXY_PREFIX + contentLocation, senders, responsePath, entry.lastOwnerID, entry.lastOwnerRevision);

            }
        }
        
        /**
         * 
         * @param reader reader to get bytes from
         */
        private static byte[]  ReadBytes(InputStreamReader reader, int length)
        {
        	byte[] arrayToFill = new byte[length];
        	try
        	{
	        	for (int i = 0; i < length; i++)
	        	{
	        		arrayToFill[i] = (byte) reader.read(); // ENSURE this is 8-bit non-UTF reading!!!!
	        	}
        	}
        	catch(IOException ex)
        	{
        		throw new RuntimeException("Unexpected IOException during ReadBytes");
        	}
        	return arrayToFill;
        }
        
        /// <summary>
        /// Handle action verbs
        /// </summary>
        /// <param name="verb">PUT, DELETE, PUSH, 305 (construct proxy path), 307 (follow proxy path)</param>
        /// <param name="reader"></param>
        /// <param name="headers">prepopulated headers from HTTP</param>
        private void HandleReadOne(String verb, String contentLocation, InputStreamReader reader, Hashtable<String, String> headers)
        {
            byte[] readData = null;
            ListInt senders = new ListInt(10);

            // do a bunch of checks before processing the packet

            // assign remote UID
            DetectBrowser(headers);

            // read data if GET request
            if (headers.containsKey("Content-Length") && !verb.equals(PUSH))
            {
                int length = Integer.parseInt(headers.get("Content-Length"));
                readData = ReadBytes(reader, length);

                if (debugBuffer != null)
                {
                    MemoryStream s = new MemoryStream(readData);
                    debugBuffer.Log(LogInstructions.DEBUG, s);
                }

            }
            else 
            {
                // no data was sent; this is a notification
            }

            // inspect the sender list
            int lastSender = 0;
            if (headers.containsKey("P2P-Sender-List"))
            {
                // save the list of senders
                senders.addAll(GetArrayOf(headers.get("P2P-Sender-List")));
                lastSender = senders.getLastItem();
            }
            else
            {
                lastSender = 0;
            }

            // inspect for a response path
            ListInt responsePath = null;
            if (headers.containsKey("P2P-Response-Path"))
            {
                responsePath =  new ListInt( GetArrayOf(headers.get("P2P-Response-Path")));
            }


            // inspect for a closing command issued by the caller,
            // which happens when this is a duplicate connection
            if (headers.containsKey("Connection"))
            {
                if (headers.get("Connection").equals("close"))
                {
                    this.state = ConnectionState.Closing;
                }
            }

            WriteDebug(this.local_uid + " read " + verb + " " +  contentLocation + " from " + this.remote_uid +  "Senders: " + headers.get("P2P-Sender-List"));

            // !senders.Contains(this.local_uid) --> if message hasn't been stamped by this node before...

            if (!senders.contains(this.local_uid) && verb.equals(DELETE) && headers.containsKey("ETag"))
            {
                HandleReadDelete(contentLocation, headers.get("ETag"), senders, responsePath);
            }
            else if (!senders.contains(this.local_uid) && contentLocation.equals(CLOSE_MESSAGE))
            {
                this.state = ConnectionState.Closing;
                this.killBit = true;
            }
            else if (!senders.contains(this.local_uid) &&  verb.equals(PUT))
            {
                HandleReadPut(contentLocation, headers.get("Content-Type"), readData, headers.get("ETag"), senders, responsePath);
            }
            else if (!senders.contains(this.local_uid) && verb.equals(PUSH))
            {
                HandleReadPush(contentLocation, headers.get("Content-Type"), headers.get("ETag"), senders, lastSender, responsePath);
            }
            else if (!senders.contains(this.local_uid) && verb.equals(POST))
            {
                HandleReadPost(contentLocation, headers.get("Content-Type"), headers.get("Accept"),  senders, readData);
            }
            else if (!senders.contains(this.local_uid) && verb.equals(RESPONSECODE_PROXY))
            {
                SendMemoryToPeer mem = RespondOrForwardProxy(PROXY_PREFIX + contentLocation, new ListInt());
                if (mem != null)
                {
                    // well, I already have the result so why is 305 being used
                    // should broadcast the result
                    //mem.Senders = new ListInt(1) { lastSender };
                    //controller.SendToPeer(mem);
                    controller.onSendToPeer(mem);
                }
                else
                {
                    // TODO: figure out what this does
                }
            }
            else if (!senders.contains(this.local_uid) && verb.equals(RESPONSECODE_PROXY2))
            {

                SendMemoryToPeer mem = RespondOrForwardProxy(contentLocation, new ListInt(senders));
                if (mem != null)
                {
                    // well, I already have the result so why is 305 being used
                    // should broadcast the result
                    //mem.Senders = new ListInt(1) { lastSender };
                    //controller.SendToPeer(mem);
                    controller.onSendToPeer(mem);
                }
                else
                {
                    // TODO: figure out what this does
                }
            }
            else if (senders.contains(this.local_uid))
            {
                // drop packet , already read
            }
            else
            {
                throw new NotImplementedException();
            }
        }

        /// <summary>
        /// Entrance for writing to a thread
        /// <returns>true if more data needs to be written</returns>
        /// </summary>
        public boolean HandleWrite()
        {
            MemoryStream bufferedOutput = null;

            
            if (client == null)
                return false;

            if (killBit)
                return false;

            if (this.state == ConnectionState.Closed || this.state == ConnectionState.Closing)
                return false;

            synchronized (sendBuffer)
            {
                if (sendBuffer.size() > 0)
                    bufferedOutput = sendBuffer.remove();
            }

            if (bufferedOutput != null)
            {
                try
                {

                	client.getOutputStream().write(bufferedOutput.getBuffer());
                	client.getOutputStream().flush();
                	
                	if (debugBuffer != null)
                    {
                        debugBuffer.Log(LogInstructions.INFO, this.local_uid + " wrote memory to " + this.remote_uid, true);
                        debugBuffer.Log(LogInstructions.DEBUG, bufferedOutput);
                    }
                    bufferedOutput.dispose();
                }
                catch(Exception ex)
                {
                }

                if (this.state == ConnectionState.WebClientConnected)
                {
                    this.state = ConnectionState.Closing;
                }
            }

            // bounded by number of dictionary entries
            // messages can still be pushed to others, especially close


            if (bufferedOutput == null)
            {
                String key="";
                synchronized (sendQueue)
                {
                    if (sendQueue.size() > 0)
                    {
                        key = sendQueue.remove();
                    }
                }

                MemoryStream srcStream=null;
                DataHeader hdr=null;


                // pull message (won't be cascading to other peers)
                synchronized (receiveEntries)
                {
                    if (receiveEntries.containsKey(key))
                    {
                        hdr = receiveEntries.get(key);
                        receiveEntries.remove(key);
                    }
                }

                // write to buffer
                if (hdr != null)
                {
                    bufferedOutput = new MemoryStream();
                    WriteSimpleGetRequest(bufferedOutput.createStreamWriter(), hdr);

                }

                // get push message
                synchronized (sendEntries)
                {
                    if (sendEntries.containsKey(key))
                    {
                        srcStream = sendEntries.get(key).MemBuffer;
                        sendEntries.remove(key);

                        if (key.equals( CLOSE_MESSAGE))
                        {
                            this.state = ConnectionState.Closing;
                        }

                    }
                }


                // package up push message
                if (srcStream != null)
                {

                    if (bufferedOutput == null)
                    {
                        bufferedOutput = new MemoryStream();
                    }
                    // srcStream.WriteTo(bufferedOutput);
                    bufferedOutput.createStreamWriter().Write(srcStream);
                }

                // wire everything off in a pair
                if (bufferedOutput != null)
                {
                    try
                    {
                        if (client != null && !killBit)//check again because other thread may have cleared the netStream
                        {
                            client.getOutputStream().write(bufferedOutput.getBuffer());
                            client.getOutputStream().flush();
                            if (debugBuffer != null)
                            {
                                debugBuffer.Log(LogInstructions.INFO, this.local_uid + " wrote " + key + " to " + this.remote_uid, true);
                                debugBuffer.Log(LogInstructions.DEBUG, bufferedOutput);
                            }
                        }
                        bufferedOutput.dispose();

                    }
                    catch(Exception ex)
                    {
                    }
                }
                    

                    
            }



            return (sendBuffer.size() > 0) || (sendEntries.size() > 0) || (receiveEntries.size() > 0);
            
        }

        // there's gotta be a better way for reading this
        // return null when file end
        private String ReadLineFromBinary(InputStreamReader reader) throws IOException
        {
            StringBuilder builder = new StringBuilder();
            
            
	            int byte1 = reader.read();
	            int byte2 = reader.read();
	
	            while (byte1 != 13 && byte2 != 10)
	            {
	                builder.append((char)byte1);
	
	                byte1 = byte2;
	                byte2 = reader.read();
	                while(byte2 == -1) // EOS
	                {
	                	try
	                	{
	                		
		                	Thread.sleep(P2PDictionary.SLEEP_IDLE_SLEEP);
		                	byte2 = reader.read();
	                	}
	                	catch(InterruptedException ex)
	                	{
	                		return builder.toString();
	                	}
	                	if (killBit) // truncate line reading
	                		return builder.toString();
	                }
	            }
            
            

            return builder.toString();
        }
        
        // return null when file end
        private String ReadLineFromBinary(InputStream reader) throws IOException
        {
            StringBuilder builder = new StringBuilder();


	            int byte1 = reader.read();
	            int byte2 = reader.read();
	
	            while (byte1 != 13 && byte2 != 10)
	            {
	                builder.append((char)byte1);
	
	                byte1 = byte2;
	                byte2 = reader.read();
	                
	                while(byte2 == -1 ) // EOS
	                {
	                	try
	                	{
	                		
		                	Thread.sleep(P2PDictionary.SLEEP_IDLE_SLEEP);
		                	byte2 = reader.read();
	                	}
	                	catch(InterruptedException ex)
	                	{
	                		return builder.toString();
	                	}
	                	if (killBit) // truncate line reading
	                		return builder.toString();
	                }

	            }
            
            
            return builder.toString(); // .substring(0, builder.length() - 2); // remove last 13-10 character combination
        }

        /// <summary>
        /// Fills bufferedOutput with the response, or asks the controller to pull the contentLocation from another peer.
        /// </summary>
        /// <param name="contentLocation">A location prefixed with /proxy.</param>
        /// <param name="requestPath">Path that the request should follow.</param>
        /// <returns>A new object to reply to the sender</returns>
        private SendMemoryToPeer RespondOrForwardProxy(String contentLocation, ListInt senderList)
        {
            // first 6 characters of /proxy are removed
            boolean proxyPart = contentLocation.startsWith(PROXY_PREFIX + "/");

            if (proxyPart == false)
                throw new NotImplementedException();

            String key = contentLocation.substring(PROXYPREFIX_REMOVE);
            ListInt hintPath = null;

            // cannnot proxy request the whole dictionary
            if (key.equals( DATA_NAMESPACE))
                throw new NotImplementedException();

            boolean responded = false;


            DataEntry e = P2PDictionary.getEntry( this.data, this.dataLock, key);
            if (e != null)
            {
                WriteDebug(this.local_uid + " following proxy path, found content for " + key);

                synchronized (e)
                {
                    if (e.subscribed && !DataMissing.isSingleton(e.value))
                    {
                        responded = true;

                    }

                }

                if (responded)
                {
                    // change the return path of the response message
                    ListInt followList = new ListInt(senderList);
                    followList.remove(this.local_uid);

                    SendMemoryToPeer sendMsg = CreateResponseMessage(key, PROXY_PREFIX + key, GetListOfThisLocalID(), followList, followList);

                    return sendMsg;
                }

                hintPath = e.senderPath;

            }

            

            if (!responded)
            {
                // fix the requestPath with the hintPath if there is no requestPath,
                // or if the requestPath is now at the current peer
                if (hintPath == null || hintPath.size()  == 0)
                {
                    WriteDebug(this.local_uid + " forwarding request dropped " + key);
                }
                else
                {

                    // since the path contains all the nodes to contact in order,
                    // we don't have to broadcast a request. Instead, we just specify
                    // the path to the next peer and it will get to the destination.
                        senderList = new ListInt(senderList);
                        senderList.add(this.local_uid);

                    
                        WriteDebug(this.local_uid + " following proxy path " + key + " to " + GetStringOf(hintPath));
                        SendMemoryToPeer sendMsg = new SendMemoryToPeer(PROXY_PREFIX + key, hintPath);
                        ResponseFollowProxy(sendMsg.MemBuffer.createStreamWriter(), PROXY_PREFIX + key, senderList);
                        return sendMsg;
                    }
                
                
               
            }

            return null;
        }


        // respond to GET/HEAD or push data on wire
        private void ResponseDictionaryText(String verb, StreamWriter writer, ListInt senderList, ListInt proxyResponsePath, boolean willClose)
        {
            String file = GetDictionaryAsTextFile();
            WriteResponseHeader(writer, DATA_NAMESPACE, "text/plain", file.length(), this.local_uid, 0, senderList, proxyResponsePath, verb, willClose);
            if (verb.equals(GET))
            {
                writer.Write(file);
            }
            writer.Flush();
            
        }

        private void ResponseDictionaryJson(String verb, StreamWriter writer, ListInt senderList, ListInt proxyResponsePath, boolean willClose)
        {
            byte[] file = GetDictionaryAsJson();
            WriteResponseHeader(writer, DATA_NAMESPACE, "application/json", file.length, this.local_uid, 0, senderList, proxyResponsePath, verb, willClose);
            if (verb.equals(GET))
            {
                writer.Write(file);
            }
            writer.Flush();
            
        }

        
        private String getFileInPackage(String filename)
        {
        	try {
                InputStream stream = Thread.currentThread().getClass().getResourceAsStream(filename);
                if (stream != null) {
                    String ret = convertStreamToString(stream);
                    stream.close();
                    return ret;
                } else {
                    return "";
                }
			} catch (IOException e) {
				// TODO Auto-generated catch block
                return "";
			}
        }
        
        // http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string
        String convertStreamToString(java.io.InputStream is) {
            try {
                return new java.util.Scanner(is).useDelimiter("\\A").next();
            } catch (java.util.NoSuchElementException e) {
                return "";
            }
        }

        private String formatString(String stringToFormat, String firstReplacement)
        {
        	return stringToFormat.replaceAll("[{]0[}]", firstReplacement);
        }
        
        private String formatString(String stringToFormat,String firstReplacement, String secondReplacement)
        {
        	return stringToFormat.replaceAll("[{]0[}]", firstReplacement).replaceAll("[{]1[}]", secondReplacement);
        }
        
        
        // default key:
        //   null -> error msg
        //   "" -> default index page
        //   * -> server redirect
        private void ResponseIndex(String verb, StreamWriter writer, boolean willClose)
        {
        	
        	String key = this.controller.getDefaultKey();
        	if (key == null)
        	{
        		WriteErrorNotFound(writer, verb, ROOT_NAMESPACE, RESPONSEVALUE_NOTFOUND);
        	}
        	else if (key.length() == 0)
        	{
	            String file = formatString(getFileInPackage(RESOURCE_INDEX), this.controller.getDescription());
	            WriteResponseHeader(writer, DATA_NAMESPACE, "text/html", file.length(), this.local_uid, 0, GetListOfThisLocalID(), null,verb, willClose);
	            if (verb.equals( GET))
	            {
	                writer.Write(file);
	            }
	            writer.Flush();
        	}
        	else
        	{
        		if ( controller.containsKey( key ) )
        		{
        			WriteErrorNotFound(writer, verb, controller.getFullKey(key), 301);
        		}
        		else
        		{
        			WriteErrorNotFound(writer, verb, ROOT_NAMESPACE, RESPONSEVALUE_NOTFOUND);
        		}
        	}
        }

        private static String Join(String joinString, List<String> stringList)
        {
        	StringBuilder bldr = new StringBuilder();
        	for( String s : stringList)
        	{
        		if (bldr.length() != 0)
        			bldr.append(joinString);
        		
        		bldr.append(s);
        	}
        	
        	return bldr.toString();
        }

        private void ResponseSubscriptions(String verb, StreamWriter writer, boolean willClose)
        {
            String file = Join("\r\n", this.keysToListen.getSubscriptionList());
            WriteResponseHeader(writer, DATA_NAMESPACE, "text/plain", file.length(), this.local_uid, 0, GetListOfThisLocalID(), null,verb, willClose);
            if (verb .equals( GET))
            {
                writer.Write(file);
            }
            writer.Flush();
        }

        private void ResponseBonjour(String verb, StreamWriter writer, boolean willClose)
        {
            StringBuilder builder = new StringBuilder();
            for (EndpointInfo conn : this.controller.getAllEndPoints())
            {
            	//("{0}\t{1}:{2}\r\n", conn.UID, conn.Address.toString(), conn.Port);
                builder.append(conn.UID);
                builder.append("\t");
                builder.append(conn.Address.toString());
                builder.append(":");
                builder.append(conn.Port);
                builder.append(NEWLINE );
            }
            String file = builder.toString();
            WriteResponseHeader(writer, DATA_NAMESPACE, "text/plain", file.length(), this.local_uid, 0, GetListOfThisLocalID(),null, verb, willClose);
            if (verb.equals( GET))
            {
                writer.Write(file);
            }
            writer.Flush();
        }

        private void ResponseConnections(String verb, StreamWriter writer, boolean willClose)
        {
            StringBuilder builder = new StringBuilder();
            for (EndPointMetadata conn : this.controller.getActiveEndPoints())
            {
                String part1 = "";
                String part2 = "";
                String part3 = "";

                if (conn.EndPoint != null)
                {
                    part1 = conn.EndPoint.toString();
                }
                else
                {
                    part1 = "disconnected";
                }
                if (conn.UID != 0)
                {
                    part2 = Integer.toString(conn.UID);
                }
                else{
                    part2 = "web-browser";
                }

                if (!conn.isServer)
                {
                    part3 = "client";
                }
                else{
                    part3 = "server";
                }
                    
                builder.append(part1);
                builder.append("\t");
                builder.append(part2);
                builder.append("\t");
                builder.append(part3);
                builder.append(NEWLINE);
                        //builder.AppendFormat("{0}\t{1}\t{2}\r\n", part1, part2, part3);
                    
                    
            }
            String file = builder.toString();
            WriteResponseHeader(writer, DATA_NAMESPACE, "text/plain", file.length(), this.local_uid, 0, GetListOfThisLocalID(),null, verb, willClose);
            if (verb.equals( GET))
            {
                writer.Write(file);
            }
            writer.Flush();
        }

        private void WriteMethodPush(String resource,ListInt senderList, ListInt proxyResponsePath, int contentLength,
            String mimeType, ETag lastVer, boolean isDeleted, boolean willClose, StreamWriter writer)
        {
            if (isDeleted)
            {
                WriteMethodDeleted(writer, resource, senderList, proxyResponsePath, lastVer.UID, lastVer.Revision);
            }
            else
            {
                WriteMethodHeader(writer, resource, mimeType, contentLength, lastVer.UID, lastVer.Revision, senderList, proxyResponsePath, willClose);
            }
        }



        ///// <summary>
        ///// respond only by header
        ///// </summary>
        ///// <param name="verb">GET or HEAD</param>
        ///// <param name="resource">Same as key or contentLocation</param>
        ///// <param name="senderList"></param>
        ///// <param name="contentLength"></param>
        ///// <param name="mimeType"></param>
        ///// <param name="lastVer"></param>
        ///// <param name="isDeleted">Indicates that the current entry is deleted</param>
        ///// <param name="willClose">Writes a close message in the header</param>
        ///// <param name="bufferedOutput">A memory buffer that will be filled with contents produced
        ///// from this method</param>
        //private void ResponseHeadStub(String verb, String resource, ListInt senderList, ListInt proxyResponsePath, int contentLength,
        //    String mimeType, ETag lastVer, bool isDeleted, bool willClose, MemoryStream bufferedOutput)
        //{
        //    StreamWriter writer = new StreamWriter(bufferedOutput, Encoding.ASCII);
            
        //        writer.NewLine = NEWLINE;

        //        if (isDeleted)
        //        {
        //            WriteResponseDeleted(bufferedOutput, resource, senderList, proxyResponsePath, lastVer.UID, lastVer.Revision);
        //        }
        //        else
        //        {
        //            WriteResponseHeader(writer, resource, mimeType, contentLength, lastVer.UID, lastVer.Revision, senderList, proxyResponsePath, verb, willClose);
        //        }
            
        //}

        // respond to a GET request, HEAD request, and push data on wire
        private void Response(String verb, String resource, ListInt senderList,ListInt proxyResponsePath,  DataEntry entry, StreamWriter writer, boolean willClose)
        {

                synchronized (entry)
                {
                    if (entry.isEmpty())
                    {
                        WriteResponseDeleted(writer, resource, senderList, proxyResponsePath, entry.lastOwnerID, entry.lastOwnerRevision);
                    }
                    else if (entry.isSimpleValue() || entry.isComplexValue())
                    {
                    	byte[] bytesToWrite = entry.writeEncodedBytes();
                        WriteResponseHeader(writer, resource, entry.getMime(), bytesToWrite.length, entry.lastOwnerID, entry.lastOwnerRevision, senderList, proxyResponsePath, verb, willClose);
                        writer.Flush();
                        if (verb.equals( GET))
                        {
                        	writer.Write(bytesToWrite);
                        }
                        writer.Flush();
                    }
                    /*
                    else if (entry.isSimpleValue() || entry.type == ValueType.String)
                    {
                        String translation ="";
                        if (entry.value != null)
                            translation = entry.value.toString();

                        byte[] bytesToWrite = System.Text.Encoding.UTF8.GetBytes(translation);

                        WriteResponseHeader(writer, resource, entry.getMime(), bytesToWrite.length, entry.lastOwnerID, entry.lastOwnerRevision, senderList, proxyResponsePath, verb, willClose);
                        writer.Flush();
                        if (verb.equals( GET))
                        {
                        	writer.Write(bytesToWrite);
                        }
                        writer.Flush();
                    }
                    else if (entry.isComplexValue())
                    {
                        if (entry.type == ValueType.Binary)
                        {
                            byte[] bentry = (byte[])entry.value;
                            WriteResponseHeader(writer, resource, entry.getMime(), bentry.length, entry.lastOwnerID, entry.lastOwnerRevision, senderList, proxyResponsePath, verb, willClose);
                            writer.Flush();
                            
                            if (verb.equals( GET))
                            {
                                writer.Write(bentry);
                            }
                            
                        }
                        else if (entry.type.equals( ValueType.Object))
                        {
                            MemoryStream bentry ;
                            try
                            {
                                bentry = new MemoryStream();
                                BinaryFormatter formatter = new BinaryFormatter();
                                formatter.Serialize(bentry, entry.value);

                                // TODO : fix this mistake in C#
                                WriteResponseHeader(writer, resource, entry.getMime(), (int)bentry.getLength(), entry.lastOwnerID, entry.lastOwnerRevision, senderList, proxyResponsePath, verb, willClose);
                                writer.Flush();

                                if (verb.equals( GET))
                                {
                                    writer.Write(bentry);
                                }

                            }
                            catch(Exception ex)
                            {
                                throw ex;
                            }


                        }
                        else
                        {
                            throw new NotImplementedException();
                        }

                    }
*/
                    else
                    {
                        throw new NotImplementedException();
                    }
                }
            
        }

        // read the format
        // key: value 
        // lines until a blank line is read
        private Hashtable<String, String> ReadHeaders(InputStreamReader reader)
        {
        	Hashtable<String, String> headers = new Hashtable<String, String>(8);

            String command ;
            
            try
            {
	            command = ReadLineFromBinary(reader);
	            while (command != null && command.length() > 0)
	            {
	                String[] read = command.split(":", 2);
	                headers.put(read[0], read[1].trim());
	
	                command = ReadLineFromBinary(reader);
	
	                if (debugBuffer != null)
	                    debugBuffer.Log(LogInstructions.DEBUG, command, false);
	            }
	
	            if (debugBuffer != null)
	                debugBuffer.Log(LogInstructions.DEBUG, command, true);
            }
            catch(IOException ex)
            {
            	// do nothing, will catch the problem when headers are read
            }

            return headers;
        }

        private static final String PERMISSION_SUBSCRIBED_MISSING = "-W";
        private static final String PERMISSION_SUBSCRIBED_AVAILABLE = "RW";
        private static final String PERMISSION_NOT_SUBSCRIBED_AVAILABLE = "=-";
        private static final String PERMISSION_NOT_SUBSCRIBED_MISSING = "--";

        /// <summary>
        /// Gets a text file that is in the following format:
        /// 
        /// First row
        ///     /data       _UID_   0       RW  _CNT_       _UID_ 
        /// Subsequent rows
        ///     /data/_key  _UID_   _REV_   RW  mime-type   _SENDERS_
        /// </summary>
        /// <returns></returns>
        private String GetDictionaryAsTextFile()
        {
            StringWriter writer = new StringWriter();

                List<DataEntry> entries; // make a local copy for access without worry about changes thereafter
                this.dataLock.readLock().lock();
                try
                {
                    entries = new Vector<DataEntry>(this.data.size());
                    //entries.AddRange(this.data.Values.Where(x => x.subscribed));
                    entries.addAll(this.data.values());
                }
                finally { this.dataLock.readLock().unlock(); }

                // write count of data entries
                writer.write(DATA_NAMESPACE + "/\t" + this.local_uid + "\t0\tRW\t" + entries.size() + "\t" + this.local_uid + NEWLINE) ;

                // write each data entry, converting simple data immediately
                // (pretend i don't know about these non-subscribed entries)
                for (DataEntry d : entries)
                {
                        String permissions = "";

                        if (d.subscribed)
                        {
                            
                            if (DataMissing.isSingleton(d.value))
                            {
                                permissions = PERMISSION_SUBSCRIBED_MISSING;
                            }
                            else
                            {
                                permissions = PERMISSION_SUBSCRIBED_AVAILABLE;
                            }
                        }
                        else
                        {

                            if (DataMissing.isSingleton(d.value))
                            {
                                permissions = PERMISSION_NOT_SUBSCRIBED_MISSING;
                            }
                            else
                            {
                                permissions = PERMISSION_NOT_SUBSCRIBED_AVAILABLE;

                            }

                        }

                        writer.write(d.key + "\t" + d.lastOwnerID + "\t" + d.lastOwnerRevision + "\t" 
                            + permissions + "\t" + d.getMime() + d.GetMimeSimpleData() + "\t"
                            + GetStringOf(d.senderPath) + NEWLINE);
                    
                }

                return writer.toString();
        }
        
        private byte[] GetEntryMetadataAsJson(DataEntry entry)
        {
        	ByteArrayOutputStream stream = new ByteArrayOutputStream();
        	JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory 
        	JsonGenerator jg;
			try {
				jg = jsonFactory.createJsonGenerator(stream, JsonEncoding.UTF8);
				WriteJSONForEntry(jg, entry);
				jg.flush();
			}
			catch(JsonGenerationException ex)
			{
				return new byte[0];
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return new byte[0];
			} // or Stream, Reader

			return stream.toByteArray();
        }
        
        private byte[] GetDictionaryAsJson()
        {
        	ByteArrayOutputStream stream = new ByteArrayOutputStream();
        	JsonFactory jsonFactory = new JsonFactory(); // or, for data binding, org.codehaus.jackson.mapper.MappingJsonFactory 
        	JsonGenerator jg;
			try {
				jg = jsonFactory.createJsonGenerator(stream, JsonEncoding.UTF8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return new byte[0];
			} // or Stream, Reader
        	

                List<DataEntry> entries; // make a local copy for access without worry about changes thereafter
                this.dataLock.readLock().lock();
                try
                {
                    entries = new Vector<DataEntry>(this.data.size());
                    //entries.AddRange(this.data.Values.Where(x => x.subscribed));
                    entries.addAll(this.data.values());
                }
                finally { this.dataLock.readLock().unlock(); }

                // write count of data entries
                //writer.write(DATA_NAMESPACE + "/\t" + this.local_uid + "\t0\tRW\t" + entries.size() + "\t" + this.local_uid + NEWLINE) ;
                try {
					jg.writeStartObject();
	                jg.writeObjectField("size", entries.size());
	                jg.writeObjectField("localid", this.local_uid);
	                
	                jg.writeArrayFieldStart("keys");
	                
	
	                // write each data entry, converting simple data immediately
	                // (pretend i don't know about these non-subscribed entries)
	                for (DataEntry d : entries)
	                {
	                    WriteJSONForEntry(jg, d);
	                }
	
	                jg.writeEndArray();
	                jg.writeEndObject();
	
	                jg.close();
				} catch (JsonGenerationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                return stream.toByteArray();
        }

private void WriteJSONForEntry(JsonGenerator jg, DataEntry d)
throws JsonGenerationException, IOException
{
	//                        writer.write(d.key + "\t" + d.lastOwnerID + "\t" + d.lastOwnerRevision + "\t" 
	  //                          + permissions + "\t" + d.getMime() + d.GetMimeSimpleData() + "\t"
	    //                        + GetStringOf(d.senderPath) + NEWLINE);

    jg.writeStartObject();
    jg.writeObjectField("key",d.key);
    jg.writeObjectField("owner",d.lastOwnerID);
    jg.writeObjectField("revision",d.lastOwnerRevision);
    jg.writeObjectField("type",d.getMime());

    if (d.subscribed)
    {
        
        if (DataMissing.isSingleton(d.value))
        {
            jg.writeObjectField("status", PERMISSION_SUBSCRIBED_MISSING);
        }
        else
        {
        	jg.writeObjectField("status", PERMISSION_SUBSCRIBED_AVAILABLE);
        }
    }
    else
    {

        if (DataMissing.isSingleton(d.value))
        {
        	jg.writeObjectField("status",PERMISSION_NOT_SUBSCRIBED_MISSING);
        }
        else
        {
        	jg.writeObjectField("status",PERMISSION_NOT_SUBSCRIBED_AVAILABLE);

        }

    }

    jg.writeEndObject();

}

        // randomize the adding so somebody will eventually win over the others since everyone wants to 
        // say that they are the "correct" one
        private int IncrementRevisionRandomizer(int originalRevision)
        {
            return originalRevision + UIDGenerator.GetNextInteger(adaptive_conflict_bound) + 1;
        }



        /// <summary>
        /// TODO: add in some code to reduce round-trips to simple data types 
        /// </summary>
        /// <param name="reader">File to read</param>
        /// <param name="sentFromList">Sent from list</param>
        /// <param name="getEntriesFromSender">This function fills in a list of entries that need to be requested from the sender</param>
        /// <param name="addEntriesToSender">These are entries that the sender does not know about</param>
        /// <seealso cref="ReadHeadStub"/>
        private void ReadDictionaryTextFile(byte[] textFile, ListInt sentFromList, List<DataHeader> getEntriesFromSender, List<SendMemoryToPeer> addEntriesToSender)
        {
            // 0 - key name
            // 1 - owner
            // 2 - revision
            // 3 - rw flag
            // 4 - MIME type

            // WriteDebug(this.local_uid + " ReadDictionaryTextFile");

        	ByteArrayInputStream reader = new ByteArrayInputStream(textFile);
        	String nsLine;
			try {
				nsLine = ReadLineFromBinary(reader);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return;
			} 
            String[] nsLineParts = nsLine.split("\t", 6);


            // if the owner of the dictionary is the same as myself, skip reading the changes
            if (nsLineParts[1].equals( Integer.toString( this.local_uid)))
            {
                throw new NotImplementedException("ReadDictionaryTextFile");// i want to see if this actually can happen (only when multiple connections happen on the same server)
              
            }

            int itemCount = Integer.parseInt(nsLineParts[4]); // count of all the items in the dictionary

            List<DataEntry> entriesCovered = new Vector<DataEntry>(itemCount + this.data.size());

            for (int i = 0; i < itemCount; i++)
            {
                try {
					nsLine = ReadLineFromBinary(reader);
				} catch (IOException e) {
					WriteDebug(getLocalUID() +  " truncated dictionary file");
					break;
				}
                nsLineParts = nsLine.split("\t", 6);
                //WriteDebug(nsLine);


                ETag tag = new ETag(Integer.parseInt(nsLineParts[1]), Integer.parseInt(nsLineParts[2]));

                // this entry is used only to call ReadMimeSimpleData
                DataEntry fakeEntry = new DataEntry("/fake", tag, new ListInt(0));
                fakeEntry.ReadMimeSimpleData(nsLineParts[4]);

                dataLock.readLock().lock();
                try
                {
                    if (this.data.containsKey(nsLineParts[0]))
                    {
                        entriesCovered.add(this.data.get(nsLineParts[0]));
                    }
                }
                finally { dataLock.readLock().unlock(); }

                // the dictionary does not report the current sender so let's tack it on
                ListInt listOfSenders = new ListInt(GetArrayOf(nsLineParts[5]));
                if (!listOfSenders.contains(this.remote_uid))
                    listOfSenders.add(this.remote_uid);

                ResponseInstruction instruction = ReadDataStub(nsLineParts[0], fakeEntry.getMime(), nsLineParts[1] + "." + nsLineParts[2], new ListInt( listOfSenders));

                if (instruction.getEntryFromSender != null)
                {
                    getEntriesFromSender.add(instruction.getEntryFromSender);
                }
                if (instruction.addEntryToSender != null)
                {
                    addEntriesToSender.add(instruction.addEntryToSender);
                }

                if (instruction.action == ResponseAction.ForwardToAll)
                {
                    listOfSenders.clear();
                }
                if (instruction.action != ResponseAction.DoNotForward)
                {
                    DataEntry get = P2PDictionary.getEntry( this.data, this.dataLock, nsLineParts[0]);

                    listOfSenders.add(this.local_uid);
                    SendBroadcastMemory msg = new SendBroadcastMemory(get.key , listOfSenders);
                    WriteMethodPush(get.key, listOfSenders, null, 0, get.getMime(), get.GetETag(), get.isEmpty(), false, msg.MemBuffer.createStreamWriter());
                    this.controller.onBroadcastToWire(msg);
                }
            }


            // now check to see which dictionary entries that the sender does not have; i'll send my entries to the caller
            this.dataLock.writeLock().lock();
            try
            {
                for (DataEntry get : this.data.values())
                {
                	if (!entriesCovered.contains(get))
                	{
	                    // i know about something that the sender does not know
	                    SendMemoryToPeer mem = new SendMemoryToPeer(get.key, sentFromList);
	                    WriteMethodPush(get.key, GetListOfThisLocalID(), null, 0, get.getMime(), get.GetETag(), get.isEmpty(), false, mem.MemBuffer.createStreamWriter());
	                    addEntriesToSender.add(mem);
                	}
                }
            }
            finally { this.dataLock.writeLock().unlock();}
        }

        /// <summary>
        ///  reads all sorts of data types
        /// </summary>
        /// <param name="contentLocation">location of the data</param>
        /// <param name="eTag">latest version of data being read</param>
        /// <param name="contentType"></param>
        /// <param name="dataOnWire">data read</param>
        /// <returns></returns>
        private ResponseAction ReadData(String contentLocation, String eTag, String contentType, ListInt senders, byte[] dataOnWire)
        {
            ResponseAction success = ResponseAction.DoNotForward;
            ETag tag = ReadETag(eTag);

            // constitute object
            DataEntry create = new DataEntry(contentLocation, tag, senders);
            create.subscribed = this.keysToListen.isSubscribed(contentLocation);
            create.ReadBytesUsingMime(contentType, dataOnWire);

            boolean upgradeable = true;
            DataEntry get = null;

            this.dataLock.writeLock().lock();
            try
            {
                if (this.data.containsKey(contentLocation))
                {
                    // update exisitng entry
                    get = this.data.get(contentLocation);
                }


                if (get == null)
                {
                    try
                    {
                        // don't save the value if not subscribed
                        if (!create.subscribed)
                        {
                            create.value = DataMissing.Singleton;
                        }

                        this.data.put(contentLocation, create);
                    }
                    finally
                    {
                        this.dataLock.writeLock().unlock();
                        upgradeable = false;
                    }

                    if (create.subscribed)
                    {
                        // notify API user
                        this.controller.onNotified(new NotificationEventArgs(create, contentLocation, NotificationReason.Add, null));
                    }

                    // never seen before, thus tell others
                    success = ResponseAction.ForwardToSuccessor;
                }
            }
            finally
            {
                if (upgradeable)
                    this.dataLock.writeLock().unlock();
            }
            
                
            if (get != null)
            {
                Object oldValue = null;

                synchronized (get)
                {
                    if (create.subscribed)
                    {

                        ETagCompare compareResult = ETag.CompareETags(create.GetETag(), get.GetETag());
                        if (compareResult == ETagCompare.FirstIsNewer || compareResult == ETagCompare.Conflict || compareResult == ETagCompare.Same)
                        {
                            oldValue = get.value;

                            if (compareResult == ETagCompare.Conflict)
                            {
                                success = ResponseAction.ForwardToAll;

                                // increment the revision and take ownership
                                create.lastOwnerID = this.local_uid;
                                create.lastOwnerRevision = IncrementRevisionRandomizer(create.lastOwnerRevision);
                            }
                            else if (DataMissing.isSingleton(oldValue))
                            {
                                success = ResponseAction.ForwardToSuccessor;
                            }
                            else if (compareResult == ETagCompare.Same)
                            {
                                success = ResponseAction.DoNotForward;
                            }
                            else//first is newer
                            {
                                success = ResponseAction.ForwardToSuccessor;
                            }


                            get.lastOwnerID = create.lastOwnerID;
                            get.lastOwnerRevision = create.lastOwnerRevision;
                            get.type = create.type;
                            get.value = create.value;

                            
                        }
                        else // SecondIsNewer
                        {
                            // return this data to the sender
                            success = ResponseAction.ForwardToAll;
                        }

                    }
                    else
                    {
                        ETagCompare compareResult = ETag.CompareETags(create.GetETag(), get.GetETag());

                        if (compareResult == ETagCompare.FirstIsNewer || compareResult == ETagCompare.Conflict || compareResult == ETagCompare.Same)
                        {
                            if (compareResult == ETagCompare.Conflict)
                            {
                                success = ResponseAction.ForwardToAll;
                            }
                            else if (compareResult == ETagCompare.Same)
                            {
                                success = ResponseAction.DoNotForward;
                            }
                            else//first is newer
                            {
                                success = ResponseAction.ForwardToSuccessor;
                            }

                            if (compareResult != ETagCompare.Same)
                            {
                                get.lastOwnerID = create.lastOwnerID;
                                get.lastOwnerRevision = create.lastOwnerRevision;
                                get.type = create.type;
                                get.value = DataMissing.Singleton;
                                get.senderPath = create.senderPath;
                            }

                        }
                        else // second is newer
                        {
                            // return this data to the sender
                            success = ResponseAction.ForwardToAll;
                        }

                    }
                }//lock


                // notify API user
                if (success != ResponseAction.DoNotForward && get.subscribed && !DataMissing.isSingleton(get.value))
                {
                    get.senderPath = create.senderPath;

                    this.controller.onNotified(new NotificationEventArgs(get,contentLocation, NotificationReason.Change, oldValue));
                }

            } // else if
            

            return success;
        }

        /// <summary>
        /// Reads data using only header information. Can be used by ReadDictionary
        /// so it handles deleted content too.
        /// </summary>
        /// <param name="contentLocation">Location of the data item without /proxy</param>
        /// <param name="contentType">GetMime()</param>
        /// <param name="eTag">Version number</param>
        /// <param name="addEntryToSender">These are entries that the sender does not know about</param>
        /// <param name="getEntryFromSender">This function fills in a list of entries that need to be requested from the sender</param>
        private ResponseInstruction ReadDataStub(String contentLocation, String contentType, String eTag, ListInt sentFromList)
        {
            ResponseInstruction success = new ResponseInstruction();
            success.action = ResponseAction.DoNotForward;
            ETag tag = ReadETag(eTag);

            DataEntry get = null;
            success.getEntryFromSender = null;
            success.addEntryToSender = null;

            // create a stub of the item
            DataEntry create = new DataEntry(contentLocation, tag, sentFromList);
            create.subscribed = keysToListen.isSubscribed(contentLocation);
            create.setMime(contentType);

            // manually erase the value (TODO, don't erase the value)
            // always default to singleton because we assume that a GET is required to complete the request
            if (create.type != ValueType.Removed)
                create.value = DataMissing.Singleton;

            boolean upgradeable = true;
            this.dataLock.writeLock().lock();
            try
            {
                if (this.data.containsKey(contentLocation))
                {
                    // update the version number of the stub
                    get = this.data.get(contentLocation);
                    
                }
                else
                {
                    try
                    {
                        this.data.put(contentLocation, create);
                    }
                    finally
                    { 
                    	this.dataLock.writeLock().unlock(); 
                    	upgradeable = false; 
                    }

                    if (create.subscribed && DataMissing.isSingleton(create.value))
                    {
                        // we'll have to wait for the data to arrive on the wire
                        // actually get the data
                        success.getEntryFromSender = new DataHeader(contentLocation, tag, sentFromList);
                        success.action = ResponseAction.DoNotForward;
                    }
                    else
                    {
                        success.action = ResponseAction.ForwardToSuccessor;
                    }

                }
            }
			catch(Exception ex)
			{
			}
            finally
            {
            	if (upgradeable)
            		this.dataLock.writeLock().unlock();
            }

            if (get != null)
            {
                synchronized (get)
                {
                    if (create.subscribed)
                    {
                        ETagCompare compareResult = ETag.CompareETags(tag, get.GetETag());

                        if (compareResult == ETagCompare.FirstIsNewer || 
                            compareResult == ETagCompare.Same || compareResult == ETagCompare.Conflict)
                        {
                			success.getEntryFromSender = new DataHeader(create.key, create.GetETag(), sentFromList);
                            success.action = ResponseAction.DoNotForward;
                        }
                        else //if (compareResult == ETagCompare.SecondIsNewer )
                        {
                            // i know about something newer than the sender, tell the sender
                            //SendMemoryToPeer mem = new SendMemoryToPeer(get.key,sentFromList);
                            //ResponseHeadStub(HEAD, get.key, GetListOfThisLocalID(), 0, get.GetMime(), get.GetETag(), get.IsEmpty, mem.MemBuffer, false);
                            //addEntryToSender = mem;

                            // well, predecessor already been handled above, so we only need to tell
                            // the others
                            success.action = ResponseAction.ForwardToAll;
                        }
                    }
                    else
                    {
                        // not subscribed
                        // just record the fact that there is newer data on the wire; cannot resolve conflicts without being a subscriber
                        ETagCompare compareResult = ETag.CompareETags(create.GetETag(), get.GetETag());
                        if (compareResult == ETagCompare.FirstIsNewer || compareResult == ETagCompare.Same || compareResult == ETagCompare.Conflict)
                        {
                            get.lastOwnerID = create.lastOwnerID;
                            get.lastOwnerRevision = create.lastOwnerRevision;
                            get.value = DataMissing.Singleton;

                            if (compareResult != ETagCompare.Same)
                            {
                                get.senderPath = create.senderPath;

                                success.action = ResponseAction.ForwardToSuccessor;
                            }
                            else
                                success.action = ResponseAction.DoNotForward;
                        }
                        else // if (compareResult == ETagCompare.SecondIsNewer )
                        {
                            //// i know about something newer than the sender
                            //SendMemoryToPeer mem = new SendMemoryToPeer(get.key,sentFromList);
                            //ResponseHeadStub(HEAD, get.key, GetListOfThisLocalID(), 0, get.GetMime(), get.GetETag(), get.IsEmpty, mem.MemBuffer, false);
                            //addEntryToSender = mem;

                            // tell the others too (already told predecessor above)
                            success.action = ResponseAction.ForwardToAll;
                        }
                    }
                }
            }

            return success;
        }

        private ListInt GetListOfThisLocalID()
        {
            return ListInt.createList( this.local_uid );
        }

        private ResponseAction ReadDelete(String contentLocation, String eTag, ListInt senderPath)
        {
            ResponseAction success = ResponseAction.DoNotForward;
            ETag tag = ReadETag(eTag);

            boolean upgradeable = true;
            this.dataLock.writeLock().lock();
            try
            {
                if (this.data.containsKey(contentLocation))
                {
                    DataEntry get = this.data.get(contentLocation);
                    Object oldValue = null;

                    // exit lock
                    this.dataLock.writeLock().unlock();
                    upgradeable = false;

                    synchronized (get)
                    {
                        ETagCompare compareResult = ETag.CompareETags(tag, get.GetETag());
                        if (compareResult == ETagCompare.FirstIsNewer || compareResult == ETagCompare.Conflict
                            || compareResult == ETagCompare.Same)
                        {
                            oldValue = get.value;

                            if (compareResult == ETagCompare.Conflict)
                            {
                                success = ResponseAction.ForwardToAll;

                                tag.UID = this.local_uid;
                                tag.Revision = IncrementRevisionRandomizer(tag.Revision);
                            }
                            else if (DataMissing.isSingleton(oldValue))
                            {
                                success = ResponseAction.ForwardToSuccessor;
                            }
                            else if (compareResult == ETagCompare.Same)
                            {
                                success = ResponseAction.DoNotForward;
                            }
                            else//first is newer
                            {
                                success = ResponseAction.ForwardToSuccessor;
                            }

                            get.lastOwnerID = tag.UID;
                            get.lastOwnerRevision = tag.Revision;
                            get.Delete();

                            if (compareResult != ETagCompare.Same)
                            {
                                get.senderPath = senderPath;
                            }

                            if (!get.subscribed)
                            {
                                get.value = DataMissing.Singleton;
                            }
                        }
                        else//if (compareResult == ETagCompare.SecondIsNewer)
                        {
                            // return to sender
                            success = ResponseAction.ForwardToAll;

                        }
                    } // end lock

                    // notify to subscribers
                    if (success != ResponseAction.DoNotForward && get.subscribed)
                        this.controller.onNotified(new NotificationEventArgs(get, "", NotificationReason.Remove, oldValue));

                }
                else
                {

                    // create a stub of the item
                    DataEntry create = new DataEntry(contentLocation, tag, senderPath);
                    create.Delete();
                    create.subscribed = keysToListen.isSubscribed(contentLocation);
                    if (!create.subscribed)
                    {
                        create.value = DataMissing.Singleton;
                    }

                    try
                    {
                        this.data.put(contentLocation, create);
                    }
                    finally
                    {
                    	// TODO: fix bug in c# where upgradeable not called
                    	this.dataLock.writeLock().unlock();
                    	upgradeable =false;
                    }

                    if (create.subscribed)
                    {
                        // notify for subscribers
                        this.controller.onNotified(new NotificationEventArgs(create, "", NotificationReason.Remove, null));
                    }

                    success = ResponseAction.ForwardToSuccessor;
                }
            }
            finally
            {
                if (upgradeable)
                	this.dataLock.writeLock().unlock();
            }

            return success;
        }


        // reads "32921.42198" and converts to two numbers
        private static ETag ReadETag(String eTag)
        {
            return new ETag(eTag);
        }

        private static String GetErrorMessage(int errorNum)
        {
            switch (errorNum)
            {
                case 200: 
                    return "OK";
                case 301: // default homepage
                	return "Moved Permanently";
                case 305: // missing
                    return "Use Proxy";
                case 404: // deleted
                    return "Not Found";
                case 405: // unused
                    return "Method Not Allowed";
                case 500: // handle read
                	return "Internal Server Error";
                case 501: // POST
                	return "Not Implemented";
                default:
                    return "Unknown";
            }
        }

        private void WriteErrorNotFound(StreamWriter writer, String verb, String contentLocation, int errorNumber)
        {
            String payload = formatString(getFileInPackage(RESOURCE_ERROR), Integer.toString( errorNumber), GetErrorMessage(errorNumber));

            writer.WriteLine("HTTP/1.1 " + Integer.toString( errorNumber) + " " + GetErrorMessage(errorNumber));
            writer.WriteLine("Content-Type: text/html");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            if (errorNumber == 301)
            	writer.WriteLine("Location: " + contentLocation);
            writer.WriteLine("Content-Length: " + Integer.toString( payload.length()));
            writer.WriteLine("Response-To: " + verb);
            writer.WriteLine(HEADER_SPECIAL + ": " + Integer.toString(this.local_uid));
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(GetListOfThisLocalID()));
            writer.WriteLine();
            writer.Write(payload);
            writer.Flush();
        }

        private void WriteErrorNotFound(StreamWriter writer, String verb, String contentLocation, int errorNumber, ListInt senderList)
        {
            String payload = formatString(getFileInPackage(RESOURCE_ERROR), Integer.toString(errorNumber), GetErrorMessage(errorNumber));

            writer.WriteLine("HTTP/1.1 " + errorNumber +  " " + GetErrorMessage(errorNumber));
            writer.WriteLine("Content-Type: text/html");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            writer.WriteLine("Content-Length: " + payload.length());
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            writer.WriteLine("Response-To: " + verb);
            writer.WriteLine(HEADER_SPECIAL + ": " + this.local_uid);
            writer.WriteLine();
            writer.Write(payload);
            writer.Flush();
        }


        private void ResponseCode(StreamWriter writer, String contentLocation, ListInt senderList, int dataOwner, int dataRevision, int code)
        {
            String payload = formatString(getFileInPackage(RESOURCE_ERROR), Integer.toString(code), GetErrorMessage(code));
            writer.WriteLine("HTTP/1.1 " + code + " " + GetErrorMessage(code));
            writer.WriteLine("Content-Type: text/html");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            writer.WriteLine("Content-Length: " + Integer.toString( payload.length()));
            writer.WriteLine(HEADER_SPECIAL + ": " + Integer.toString( this.local_uid));
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            writer.WriteLine("ETag: \"" + dataOwner + "." + dataRevision + "\"");
            writer.WriteLine("Response-To: GET");
            writer.WriteLine();
            writer.Write(payload);
            writer.Flush();
        }

        private void ResponseFollowProxy(StreamWriter writer, String contentLocation, ListInt senderList)
        {
            final int code = 307;
            String payload = formatString(getFileInPackage(RESOURCE_ERROR), Integer.toString(code), GetErrorMessage(code));

            writer.WriteLine("HTTP/1.1 " + code + " " + GetErrorMessage(code));

            writer.WriteLine("Content-Type: text/html");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            writer.WriteLine("Content-Length: " + Integer.toString( payload.length()));
            writer.WriteLine(HEADER_SPECIAL + ": " +  Integer.toString(this.local_uid));
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            writer.WriteLine("ETag: \"" + 0 + "." + 0 + "\"");
            writer.WriteLine("Response-To: GET");
            writer.WriteLine();
            writer.Write(payload);
            writer.Flush();
        }

        private void WriteResponseInfo(StreamWriter writer, String contentLocation,DataEntry entry)
        {
        	int code = 200;
			byte[] payload = GetEntryMetadataAsJson(entry);
        	
        	writer.WriteLine("HTTP/1.1 " + code + " " + GetErrorMessage(code));
        	writer.WriteLine("Content-Type: application/json");
        	writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
        	writer.WriteLine("Content-Length: " + Integer.toString(payload.length));
        	writer.WriteLine();
        	writer.Write(payload);
        	writer.Flush();
        }


        private void WriteResponseDeleted(StreamWriter writer, String contentLocation, ListInt senderList, ListInt proxyResponsePath, int dataOwner, int dataRevision)
        {
            String payload = formatString(getFileInPackage(RESOURCE_ERROR), "404", GetErrorMessage(404));
            writer.WriteLine("HTTP/1.1 404 Not Found");
            writer.WriteLine("Content-Type: text/html");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            writer.WriteLine("Content-Length: " + Integer.toString(payload.length()));
            writer.WriteLine(HEADER_SPECIAL + ": " + Integer.toString(this.local_uid));
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            if (proxyResponsePath != null)
            {
                writer.WriteLine("P2P-Response-Path: " + GetStringOf(proxyResponsePath));
            }
            writer.WriteLine("ETag: \"" + dataOwner + "." + dataRevision + "\"");
            writer.WriteLine("Response-To: GET");
            writer.WriteLine();
            writer.Write(payload);
            writer.Flush();
        }

        /// <summary>
        /// REST request method to delete a resource
        /// </summary>
        /// <param name="stream">writer stream</param>
        /// <param name="contentLocation">resource</param>
        /// <param name="senderList"></param>
        /// <param name="proxyResponsePath"></param>
        /// <param name="dataOwner">resource version</param>
        /// <param name="dataRevision">resource version</param>
        private void WriteMethodDeleted(StreamWriter writer, String contentLocation, ListInt senderList, ListInt proxyResponsePath, int dataOwner, int dataRevision)
        {
            writer.WriteLine(DELETE + " " + URLUtils.URLEncode(contentLocation) + " HTTP/1.1");
            writer.WriteLine(HEADER_SPECIAL + ": " + Integer.toString(this.local_uid));
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            if (proxyResponsePath != null)
            {
                writer.WriteLine("P2P-Response-Path: " + GetStringOf(proxyResponsePath));
            }
            writer.WriteLine("ETag: \"" + dataOwner + "." + dataRevision + "\"");
            writer.WriteLine();
            writer.Flush();
        }

        private static ListInt GetArrayOf(String integerList)
        {
            if (integerList.length() == 0)
                return new ListInt(0);
            
            String[] strSenders = integerList.split(",");
            ListInt list = new ListInt(strSenders.length);
            for (String s : strSenders)
            {
            	list.add(Integer.parseInt(s));
            }
            return list;
        }


        // converts a list of numbers into
        // 1,2,3
        private static String GetStringOf(ListInt senderList)
        {
            if (senderList.size() == 0)
                return "";

            StringBuilder str = new StringBuilder();
            for (int i : senderList)
            {
                str.append(i);
                str.append(",");
            }

            // remove last comma if exists
            if (str.length() > 0)
            	return str.substring(0, str.length() - 1);
            else
            	return str.toString();
        }

        private void WriteMethodHeader(StreamWriter writer, String contentLocation, String contentType, int contentSize, 
            int dataOwner, int dataRevision, ListInt senderList, ListInt responsePath, boolean willClose)
        {
            writer.WriteLine( PUSH + " " + URLUtils.URLEncode(contentLocation) + " HTTP/1.1");
            writer.WriteLine(HEADER_SPECIAL + ": " + this.local_uid);
            writer.WriteLine("ETag: \"" + dataOwner + "." + dataRevision + "\"");
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            if (responsePath != null)
            {
                writer.WriteLine("P2P-Response-Path: " + GetStringOf(responsePath));
            }
            writer.WriteLine("Content-Type: " + contentType);
            writer.WriteLine("Content-Length: " + Integer.toString( contentSize));
           
            if (willClose)
            {
                writer.WriteLine("Connection: close");
            }

            writer.WriteLine();
            writer.Flush();
        }

        /// <summary>
        /// 
        /// </summary>
        /// <param name="writer"></param>
        /// <param name="contentLocation"></param>
        /// <param name="contentType"></param>
        /// <param name="contentSize"></param>
        /// <param name="dataOwner"></param>
        /// <param name="dataRevision"></param>
        /// <param name="senderList"></param>
        /// <param name="responsePath">Can be null.</param>
        /// <param name="responseToVerb"></param>
        /// <param name="willClose"></param>
        private void WriteResponseHeader(StreamWriter writer, String contentLocation, String contentType, int contentSize, 
            int dataOwner, int dataRevision, ListInt senderList, ListInt responsePath, String responseToVerb, boolean willClose)
        {
            writer.WriteLine("HTTP/1.1 200 OK");
            writer.WriteLine(HEADER_SPECIAL + ": " + this.local_uid);
            writer.WriteLine("ETag: \"" + dataOwner + "." + dataRevision + "\"");
            writer.WriteLine(HEADER_LOCATION + ": " + contentLocation);
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(senderList));
            if (responsePath!=null)
            {
                writer.WriteLine("P2P-Response-Path: " + GetStringOf(responsePath));
            }
            writer.WriteLine("Content-Type: " + contentType);
            writer.WriteLine("Content-Length: " + Integer.toString( contentSize));
            if (responseToVerb.length() > 0)
            {
                writer.WriteLine("Response-To: " + responseToVerb);
            }
            if (willClose)
            {
                writer.WriteLine("Connection: close");
            }

            writer.WriteLine();
            writer.Flush();
        }

        private void WriteSimpleGetRequest(StreamWriter writer, DataHeader request)
        {
            writer.WriteLine(GET + " " + URLUtils.URLEncode( request.key) + " HTTP/1.1");
            writer.WriteLine("P2P-Sender-List: " + GetStringOf(request.sentFrom));
            writer.WriteLine(HEADER_SPECIAL + ": " + this.local_uid);
            writer.WriteLine();
            writer.Flush();
        }

}

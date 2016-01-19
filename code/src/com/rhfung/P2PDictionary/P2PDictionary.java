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
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rhfung.Interop.EndPoint;
import com.rhfung.Interop.EndPointMetadata;
import com.rhfung.Interop.ListInt;
import com.rhfung.Interop.NotSupportedException;
import com.rhfung.Interop.TcpClient;
import com.rhfung.P2PDictionary.callback.DefaultCallback;
import com.rhfung.P2PDictionary.callback.IDictionaryCallback;
import com.rhfung.P2PDictionary.peers.NoDiscovery;
import com.rhfung.P2PDictionary.peers.PeerDiscovery;
import com.rhfung.P2PDictionary.peers.PeerInterface;
import com.rhfung.logging.LogInstructions;


/**
     * Main class for connecting to the P2P dictionary.
     * @author Richard
     *
     */
    public class P2PDictionary implements IMessageController, ISubscriptionChanged, Map<String, Object>
    {
        static final String DATA_NAMESPACE = DataConnection.DATA_NAMESPACE; // /BLAH
        static final  int MAX_RANDOM_NUMBER = 10;

        static final  int SENDER_THREADS = 3;
        static final  int BACKLOG = 2048;
        static final  int SIMULATENOUS_REQUESTS = 4;

        static final  int SLEEP_WAIT_TO_CLOSE = 100;
        static final  int SLEEP_IDLE_SLEEP = 8;
        static final  int SLEEP_USER_RETRY_READ = 30;
        
        volatile boolean killbit = false;
        volatile boolean killbitSenderThreads = false;

        Thread runLoop;
        Thread[] senderThreads;
        
        Timer constructNwTimer;
        ServerSocket listener; 

        Hashtable<String, DataEntry> data;
        Hashtable<Integer, Integer> messages;
        ReadWriteLock dataLock;

        List<DataConnection> connections;
        Subscription subscription;
        PeerDiscovery discovery;

        int constructNwNextPeer=0;
        int constructNwRandomPeer=0;

        int mSearchForClientsTimeout = 0;
        
        int _localUID;
        String _description;
        String _partition;
        String _defaultKey;

        //http://msdn.microsoft.com/en-us/library/system.idisposable.aspx

        LogInstructions debugBuffer;

        IDictionaryCallback callback;

        /**
         * Creates a dictionary object.
         */
        public static class Builder {
            private String m_description = "";
            private int m_port = 8765;
            private String m_namespace = "default";
            private P2PDictionaryServerMode m_serverMode = P2PDictionaryServerMode.AutoRegister;
            private P2PDictionaryClientMode m_clientMode = P2PDictionaryClientMode.AutoConnect;
            private int m_clientSearchTimespan = 5000;
            private IDictionaryCallback m_callback = new DefaultCallback();
            private PeerInterface m_discovery = new NoDiscovery();

            /**
             * User-friendly description of the dictionary to appear on its website
             * @param description
             */
            public void setDescription(String description) {
                m_description = description;
            }

            /**
             * Port for binding a server
             * @param port
             */
            public void setPort(int port) {
                m_port = port;
            }

            /**
             * Namespace of the dictionary, must be IDENTICAL between all connected peers.
             * @param namespace
             */
            public void setNamespace(String namespace) {
                m_namespace = namespace;
            }

            /**
             * Can start the server on constructor or OpenServer, or not at all. Can open start server once.
             * @param serverMode
             */
            public void setServerMode(P2PDictionaryServerMode serverMode){
                m_serverMode = serverMode;
            }

            /**
             * Determines if other dictionary peers are connected automatically using searchForClientsTimespan
             * @param clientMode
             */
            public void setClientMode(P2PDictionaryClientMode clientMode) {
                m_clientMode = clientMode;
            }

            /**
             * Duration for dictionary peers to connect automatically
             * @param timespan
             */
            public void setClientSearchTimespan(int timespan) {
                m_clientSearchTimespan = timespan;
            }

            /**
             * Callback object to handle callbacks from the dictionary on alternate threads
             * @param callback
             */
            public void setCallback(IDictionaryCallback callback) {
                m_callback = callback;
            }

            /**
             * Backend for discovering peers.
             * @param discovery
             */
            public void setPeerDiscovery(PeerInterface discovery) {
                m_discovery = discovery;
            }

            public P2PDictionary build() {
                return new P2PDictionary(m_description,
                        m_port,
                        m_namespace,
                        m_serverMode,
                        m_clientMode,
                        m_clientSearchTimespan,
                        m_callback,
                        m_discovery
                );
            }
        }

        /**
         * Creates a dictionary object.
         * @param description User-friendly description of the dictionary to appear on its website
         * @param port Port for binding a server
         * @param ns Namespace of the dictionary, must be IDENTICAL between all connected peers.
         * @param serverMode Can start the server on constructor or OpenServer, or not at all. Can open start server once.
         * @param clientMode Determines if other dictionary peers are connected automatically using searchForClientsTimespan
         * @param searchForClientsTimespan time in milliseconds
         */
        public P2PDictionary(String description,  int port, String ns,
                P2PDictionaryServerMode serverMode,
                P2PDictionaryClientMode clientMode,
                int searchForClientsTimespan)
        {
        	this(description, port, ns, serverMode, clientMode, searchForClientsTimespan, new DefaultCallback());
        }

        /**
         * Creates a dictionary object.
         * @param description User-friendly description of the dictionary to appear on its website
         * @param port Port for binding a server
         * @param namespace Namespace of the dictionary, must be IDENTICAL between all connected peers.
         * @param serverMode Can start the server on constructor or OpenServer, or not at all. Can open start server once.
         * @param clientMode Determines if other dictionary peers are connected automatically using searchForClientsTimespan
         * @param searchForClientsTimespan time in milliseconds
         * @param cb Callback object to handle callbacks from the dictionary on alternate threads
         */
        public P2PDictionary(String description,
                             int port,
                             String namespace,
                             P2PDictionaryServerMode serverMode,
                             P2PDictionaryClientMode clientMode,
                             int searchForClientsTimespan,
                             IDictionaryCallback cb)
        {
            this(description, port, namespace,
                    serverMode, clientMode, searchForClientsTimespan,
                    new DefaultCallback(), new NoDiscovery());
        }

        private P2PDictionary(String description,
                             int port,
                             String namespace,
                             P2PDictionaryServerMode serverMode,
                             P2PDictionaryClientMode clientMode,
                             int searchForClientsTimespan,
                             IDictionaryCallback cb,
                             PeerInterface discovery)
        {
            // some random ID
            this._description = description;
            this._localUID = UIDGenerator.GetNextInteger();
            this._partition = namespace;
            this._defaultKey = "";

            // load data from caller
            this.data = new Hashtable<String, DataEntry>();
            this.dataLock = new ReentrantReadWriteLock();
            this.messages = new Hashtable<Integer, Integer>();

            this.connections = new Vector<DataConnection>();
            this.subscription = new Subscription(this);

            this.discovery = new PeerDiscovery(discovery);
            this.callback  = cb;

            // sender threads
            ConstructSenderThreads();

            // okay, some auto startup
            if (serverMode == P2PDictionaryServerMode.AutoRegister || serverMode == P2PDictionaryServerMode.Hidden)
            {
                try {
					this.openServer(port);
				} catch (NotSupportedException e) {
					callback.Disconnected(new ConnectionEventArgs(null, this._localUID));
				} catch (IOException e) {
					callback.Disconnected(new ConnectionEventArgs(null, this._localUID));
				}
                
                if (serverMode == P2PDictionaryServerMode.AutoRegister)
                {
                    this.discovery.RegisterServer(this);
                }
            }

            if (clientMode == P2PDictionaryClientMode.AutoConnect)
            {
                this.discovery.BrowseServices();
                mSearchForClientsTimeout = searchForClientsTimespan;
                
                constructNwTimer  = new Timer(true);
                constructNwTimer.schedule(new TimerTask() {
    				
    				@Override
    				public void run() {
    		            ConstructNetwork();
    				}
    			}, mSearchForClientsTimeout, mSearchForClientsTimeout);

            }

            this.debugBuffer = null;
        }

        /**
         * Closes the dictionary
         */
        @Override
        protected void finalize()
        {
        	try
        	{
        		close(true);
        	}
        	finally
        	{
        	}
        }
       
        /**
         * Sets a callback. Callback methods run on different threads.
         * Be careful not to block or throw an exception.
         * @param newCallback null or object
         */
        public void setCallback(IDictionaryCallback newCallback)
        {
        	if (newCallback == null)
        	{
        		this.callback = new IDictionaryCallback() {
					
					@Override
					public void SubscriptionChanged(SubscriptionEventArgs e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void Notification(NotificationEventArgs e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void Disconnected(ConnectionEventArgs e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void ConnectionFailure(ConnectionEventArgs e) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void Connected(ConnectionEventArgs e) {
						// TODO Auto-generated method stub
						
					}
				};
        	}
        	else
        	{
        		this.callback = newCallback;
        	}
        }
        
        /**
         * Returns a unique ID number for the dictionary. Should be unique to all peers.
         * @return
         */
        public int getLocalID()
        {
            
                return this._localUID;
            
        }

        /// <summary>
        /// 
        /// </summary>
        List<DataConnection> getConnections()
        {
            
                return this.connections;
            
        }
        
        /**
         * Name of the dictionary partition. Only data is accessible if in the same partition.
         * @return
         */
        public String getNamespace()
        {
            
                return this._partition;
            
        }
    

        public PrintStream getDebugBuffer()
        {
        	if (this.debugBuffer !=null)
            return this.debugBuffer.GetTextWriter();
        	else
        		return null;
        }
        
        /**
         * Configures logging with only important messages. Flushes log after every message
         * @param debugBuffer null to disable
         */
        public void setDebugBuffer(PrintStream debugBuffer)
        {
        	setDebugBuffer(debugBuffer, LogInstructions.INFO, true);
        }
        

        /**
         * Configures logging.
         * @param writer null to disable
         * @param level 0 = all in/out messages, 1 = connection messages only
         * @param autoFlush flush writer stream after every log message
         */
        public void setDebugBuffer(PrintStream  writer, int level, boolean autoFlush)
        {
            if (writer == null)
                this.debugBuffer = null;
            else
                this.debugBuffer = new LogInstructions(writer, level, autoFlush);

            synchronized (connections)
            {
                for (DataConnection c : connections)
                {
                    c.debugBuffer = this.debugBuffer;
                }
            }
        }

        /**
         * Describes the user-friendly name of this P2P client for web browser requests.
         */
        public String getDescription()
        {
            
                return this._description;
            
        }

       /**
        *  Returns the IP address of the server. Cast return value as System.Net.IPEndPoint.
        * @return
        */
        public EndPoint getLocalEndPoint()
        {
        
            if (this.listener != null)
                return new EndPoint(this.listener.getInetAddress(), this.listener.getLocalPort());
            else
                return null;
            
        }
        
        /**
         * When the web interface's root key is called, the value for the following
         * key is returned instead. Allows a web browser to automatically redirect
         * for return code 301. 
         * @param key string to use the default homepage; null to disable homepage
         */
        public void setDefaultKey(String key)
        {
        	this._defaultKey = key;
        }
        
        /**
         * Returns the default key for the web interface's root resource (/). 
         * @return key in the dictionary's namespace
         */
        public String getDefaultKey()
        {
        	return this._defaultKey;
        }


        // thread safe
        public boolean containsKey(String key)
        {
            // return data.ContainsKey(GetFullKey(DATA_NAMESPACE ,key));
            DataEntry e = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
            if (e == null)
                return false;
            else
                return e.subscribed;
        }

        static String GetFullKey(String ns, String partition, String key)
        {
        	return GetFullKeyBypass(ns, partition, key);
        }
        
        static String GetFullKeyBypass(String ns, String partition, String key)
        {
            if (key.startsWith(ns))
                throw new IndexOutOfBoundsException("GetFullKey should not operate on keys within namespace");
            return ns + "/" + partition + "/" + key; 
        }

        static String GetUserKey(String ns, String partition, String fullKey)
        {
            if (IsFullKeyInNamespace(ns,partition,fullKey))
                return fullKey.substring(ns.length() + 1 + partition.length() + 1);
            else
                throw new IndexOutOfBoundsException("GetUserKey should not operate on keys outside of namespace");
        }

        static boolean IsFullKeyInNamespace(String ns, String partition, String fullKey)
        {
            return fullKey.startsWith(ns + "/" + partition + "/");
        }
        
        /// <summary>
        /// 
        /// </summary>
        /// <param name="data"></param>
        /// <param name="rwl"></param>
        /// <param name="fullKey"></param>
        /// <returns></returns>
        /// <remarks>called in both P2PDictionary and DataConnection</remarks>
        static DataEntry GetEntry(Map<String,DataEntry> data, ReadWriteLock rwl, String fullKey)
        {
            rwl.readLock().lock();
            DataEntry entry = null;
            try
            {
                
                if (data.containsKey(fullKey))
                {
                    entry = data.get(fullKey);
                }
            }
            finally
            {
                rwl.readLock().unlock();
            }

            return entry;
        }


       /**
        * Blocking call to read from the dictionary, throws IndexOutOfRangeException
        * @param key
        * @param msTimeout
        * @return
        * @throws SubscriptionException
        */
        public  Object getWithTimeout(String key, int msTimeout ) throws SubscriptionException
        {
            int sleepLength = 0;
            DataEntry e = GetEntry(this.data, this.dataLock ,GetFullKey(DATA_NAMESPACE, _partition, key));
            while(sleepLength < msTimeout && e == null)
            {
            	try
            	{
	                Thread.sleep(SLEEP_USER_RETRY_READ);
	                e = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
	                sleepLength += SLEEP_USER_RETRY_READ;
            	}
            	catch(Exception ex)
            	{
            		break;
            	}
            }
            if (e == null)
            {
                throw new IndexOutOfBoundsException("No dictionary element with the key exists");
            }
            if (!e.subscribed)
            {
                throw new SubscriptionException("Not subscribed to key");
            }
            return e.value;
        }

		/**
		 * Blocking call to read from the dictionary, waits for msTimeout, returns false if cannot get the value
		 * @param key
		 * @param msTimeout timeout in milliseconds
		 * @param defaultValue returns this value if not able to get the key from the dictionary
		 * @return
		 */
        public Object tryGetValue(String key, int msTimeout, Object defaultValue )
        {
            int sleepLength = 0;
            DataEntry e = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
            while (sleepLength < msTimeout && e == null)
            {
            	try
            	{
	                Thread.sleep(P2PDictionary.SLEEP_USER_RETRY_READ);
	                e = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
	                sleepLength += P2PDictionary.SLEEP_USER_RETRY_READ;
            	}
            	catch(InterruptedException ex)
            	{
            		break;
            	}
            }
            if (!e.subscribed)
            {
                return defaultValue;
            }
            return e.value;
        }

        /**
         * 
         * @param key  Unique string, cannot contain regular expression control characters.
         * @return Object if in dictionary
         * @throws SubscriptionException if the key is not subscribed previously (or added with put)
         */
        // thread safe
        public Object get(String key) throws SubscriptionException
        {
            
                // todo: decide to crash or return null
                DataEntry e = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
                if (!e.subscribed)
                {
                    throw new SubscriptionException("Not subscribed to key");
                }
                return e.value;
        }
        
        
         /**
          * @param key Unique string, cannot contain regular expression control characters. 
          * @param value 
          */
        @Override
        public Object put(String key, Object value)
            
        {
            boolean upgraded = true;
            DataEntry get = null;
            NotificationReason reason;
            Object oldValue = null;

            dataLock.writeLock().lock();

            try
            {
                
                if (this.data.containsKey(GetFullKey(DATA_NAMESPACE, _partition, key)))
                    get = this.data.get(GetFullKey(DATA_NAMESPACE, _partition, key));

                if (get != null)
                {
                    // leave the lock now
                    dataLock.writeLock().unlock();
                    upgraded = false;


                    // work with the dictionary entry
                    reason = NotificationReason.Change;
                    synchronized (get)
                    {
                        oldValue = get.value; // save old value for notificatoin event

                        DataEntry entry = data.get((GetFullKey(DATA_NAMESPACE, _partition, key)));
                        
                        get.lastOwnerID = _localUID;
                        get.lastOwnerRevision = entry.lastOwnerRevision + 1;
                        get.value = value;
                        get.subscribed = true;
                        get.senderPath = ListInt.createList( this._localUID );

                        get.DetectTypeFromValue();
                    }
                    // if some pattern does not have this subscription, then add it automatically
                    if (!subscription.isSubscribed(GetFullKey(DATA_NAMESPACE, _partition, key)))
                    {
                        addSubscription(key);
                    }
                }
                else
                {
                    

                    try
                    {
                        reason = NotificationReason.Add;
                        get = new DataEntry(GetFullKey(DATA_NAMESPACE, _partition, key), value, new ETag(_localUID, 0), ListInt.createList( this._localUID ), true);

                        data.put(GetFullKey(DATA_NAMESPACE, _partition, key), get);
                    }
                    finally 
                    {
                        dataLock.writeLock().unlock();
                        upgraded = false;
                    }

                    // if some pattern does not have this subscription, then add it automatically
                    if (!subscription.isSubscribed(GetFullKey(DATA_NAMESPACE, _partition, key)))
                    {
                        AddSubscription(key, SubscriptionInitiator.AutoAddKey);
                    }
                }

            }
            finally
            {
                if (upgraded)
                    dataLock.writeLock().unlock();
            }

            // send data outside on each connection
            // ask any connection to formulate a message
            // and then we handle sending it here
            if (connections.size() > 0 && !this.killbit)
            {
                SendMemoryToPeer msg = connections.get(0).CreateResponseMessage(GetFullKey(DATA_NAMESPACE, _partition, key));
                SendBroadcastMemory msg2 = new SendBroadcastMemory(msg.ContentLocation, ListInt.createList(this.getLocalID()));
                msg2.MemBuffer = msg.MemBuffer;
                onBroadcastToWire(msg2);
            }

            // notify local loopback
            NotificationEventArgs args = new NotificationEventArgs(get, key, reason, oldValue);
            callback.Notification(args);
            
        
            return oldValue;
        }
    
        /**
         * Removes all keys that are currently owned by this peer.
         */
        public void clear()
        {
            dataLock.writeLock().lock();
            try
            {
                for (DataEntry entry : this.data.values())
                {
                    if (entry.lastOwnerID == this._localUID)
                    {
                        remove(entry.key);
                    }
                }
            }
            finally
            {
                dataLock.writeLock().unlock();
            }

        }

        
        /**
         * Removes a dictionary entry
         * @param key
         * @return
         */
        public Object remove(String key)
        {
            DataEntry get = GetEntry(this.data, this.dataLock, GetFullKey(DATA_NAMESPACE, _partition, key));
            if (get != null)
            {
            	Object lastValue = null;
                synchronized (get)
                {
                    get.lastOwnerID = _localUID;
                    get.lastOwnerRevision = data.get(GetFullKey(DATA_NAMESPACE, _partition, key)).lastOwnerRevision + 1;
                    lastValue = get.value;
                    get.Delete();
                    get.subscribed = true;
                    get.senderPath = ListInt.createList( this._localUID );
                }

                // if some pattern does not have this subscription, then add it automatically
                if (!subscription.isSubscribed(GetFullKey(DATA_NAMESPACE, _partition, key)))
                {
                    addSubscription(key);
                }

                // send data outside on each connection
                // ask any connection to formulate a message
                // and then we handle sending it here
                if (connections.size() > 0 && !this.killbit)
                {
                    SendMemoryToPeer msg = connections.get(0).CreateResponseMessage(GetFullKey(DATA_NAMESPACE, _partition, key));
                    SendBroadcastMemory msg2 = new SendBroadcastMemory(msg.ContentLocation, ListInt.createList(this.getLocalID()));
                    msg2.MemBuffer = msg.MemBuffer;
                    onBroadcastToWire(msg2);
                }
                
                return lastValue;
            }
            else
            {
                return null;
            }

            
        }


        /**
         *  Returns number of dictionary entries including non-subscribed entries
         */
        public int size()
        {
        
            int retValue  = 0;
            dataLock.readLock().lock();
            try
            {
            	for(String k : this.data.keySet())
            	{
            		if (IsFullKeyInNamespace(DATA_NAMESPACE, _partition, k))
            			retValue++;
            	}
            }
            finally { dataLock.readLock().unlock(); }
            return retValue;
        
        }

        /**
         * Opens a server and binds to all interfaces.
         * @param port open port number
         * @throws NotSupportedException
         * @throws IOException
         */
        public void openServer(int port) throws NotSupportedException, IOException
        {
        	openServer(null, port);
        }
        
		/**
		 * Opens a server and binds to the specific port. Only one server instance can be run at a time.
		 * @param addr server interface to bind to
		 * @param port port number to listen
		 * @throws NotSupportedException
		 * @throws IOException
		 */
        public void openServer(java.net.InetAddress addr, int port) throws NotSupportedException, IOException
        {
            if (runLoop != null)
            {
                throw new NotSupportedException("Cannot run the server more than once");
            }

            if (addr == null)
            {
            	listener = new ServerSocket(port, BACKLOG);
            }
            else{
	            // open connection
	            listener = new ServerSocket(port, BACKLOG, addr);
            }
            listener.setSoTimeout(1000); // allows accept() to loop

            // fake local UID
            //this._localUID = port;

            // start
            runLoop = new Thread(new Runnable() {
				
				@Override
				public void run() {
					  	while(!killbit)
			            {
						  try
						  {
							  final Socket s = listener.accept();
							  final DataConnection conn = new DataConnection(ConnectionType.Server,  getLocalID(), data, dataLock, new WeakDataServer(P2PDictionary.this), subscription, debugBuffer);
			                    
							  final Thread t = new Thread(new Runnable() {
								
								@Override
								public void run() {
						            synchronized (connections)
						            {
						                connections.add(conn);
						            }

						            try
						            {
						            	TcpClient client = new TcpClient(s);
						                conn.ReadLoop(client);
						            }
						            catch(Exception ex)
						            {
						            	WriteDebug(_localUID +  " Exception on server receiving client: " );
						            	if (debugBuffer !=null)
						            		ex.printStackTrace(debugBuffer.GetTextWriter());
						            }
						            finally
						            {
						                synchronized (connections)
						                {
						                    getConnections().remove(conn);
						                }
						            }
									
								}
							});

					            conn.setThread(t);
			                    t.setDaemon(true);
			                    WriteDebug(_localUID + " Server: Accepting connection...");
			                    t.setName(getDescription() + " Server thread " + s.getRemoteSocketAddress().toString());
			                    t.start();
			                    WriteDebug(_localUID + " Server: Connection opened");
							  

						  }
						  catch(SocketTimeoutException ex)
						  {
							  // ignore
						  }
						  catch(IOException ex)
						  {
							  WriteDebug(_localUID + " failed to accept incoming connection: " + ex.getMessage() );
						  }
						  
			            }

					  // close the listener
			            try {
							listener.close();
							WriteDebug(_localUID + " server closed");
						} catch (IOException e) {
						}
					
				}
			});
            runLoop.setDaemon(true);
            runLoop.setName(this.getDescription() + " listener " + listener.getInetAddress().toString());
            runLoop.start();
        }


        /// <summary>
        /// Aborts all connection and server threads immediately.
        /// </summary>
        public void abort()
        {
            if (this.discovery != null)
            {
                this.discovery.UnregisterServer();
                this.discovery = null;
            }

            // stop receving connections
            runLoop.interrupt();
            runLoop = null;

            // end each connection
            for (DataConnection c : connections)
            {
                c.Kill();
            }

  
        }


        /// <summary>
        /// Closes all connections when data is finished being served.
        /// </summary>
        public void close()
        {
            close(false);
        }

        private void close(boolean disposing)
        {
            // stop listener
            killbit = true;


            // stop auto connect
            if (this.constructNwTimer != null)
            {
                WriteDebug("Stopping auto-connect timer");
                this.constructNwTimer.cancel();
                this.constructNwTimer = null;
            }

            // disconnect discovery
            if (this.discovery != null)
            {
                WriteDebug("Unregistering discovery");
                this.discovery.UnregisterServer();
                this.discovery = null;
            }

            if (!disposing)
            {
                if (this.runLoop != null)
                {
                    WriteDebug("Waiting for run loop to close");
                	try
                	{
                		runLoop.join(1000);
                	}
                	catch(InterruptedException ex)
                	{
                	}
                	finally
                	{
                        if (runLoop.isAlive()) {
                            WriteDebug("Killing run loop");
                            runLoop.interrupt();
                        }
                		runLoop = null;
                	}
                }
            }

            List<DataConnection> closeConn = new Vector<DataConnection>(connections);

            // close all reader connections
            for (DataConnection c : closeConn)
            {
                WriteDebug("Closing data connection");
                c.Close(disposing);
            }

            // stop sending all data on sender threads
            killbitSenderThreads = true;

            
            if (!disposing)
            {
                for (Thread thd : senderThreads)
                {
                    if (thd.isAlive())
                    {
                        WriteDebug("Waiting for sender thread to close " + thd.getName());
                    	// idle wait for thread to close
                    	try
                    	{
                    		thd.join(1000);
                    	}
                    	catch(InterruptedException ex)
                    	{

                    	}
                        if (thd.isAlive()) {
                            thd.interrupt();
                            WriteDebug("Killing sender thread " + thd.getName());
                        }
                    }
                }
            }
        }

        
        /**
         * Manually connect to another peer. Not guaranteed to actually connect to the client
         * because it may be already connected or unreachable.
         * @param addr
         * @param port
         * @return true if a TCP connection to the client is possible, false otherwise
         */
        public boolean OpenClient(final java.net.InetAddress addr, final int port)
        {	
        	
        	try
        	{
	        	final Socket s = new Socket(addr, port);
	        	final DataConnection conn  = new DataConnection(ConnectionType.Client, getLocalID(), data, dataLock, new WeakDataServer(P2PDictionary.this), subscription, debugBuffer);
	        	// start
	            final Thread runLoop = new Thread(new Runnable() {
					
					@Override
					public void run() {
			            
			            synchronized (connections)
			            {
			                getConnections().add(conn);
			            }
	
			            try
			            {
			                TcpClient client = new TcpClient(s);
	
			                // updates are always made by the "CLIENT"
			                conn.AddRequestDictionary();
			                conn.ReadLoop(client);
			                
			            }
			            catch(Exception ex)
			            {
			                WriteDebug(Integer.toString( getLocalID()) + " Exception on read loop" );
			                if (debugBuffer != null)
			                	ex.printStackTrace(debugBuffer.GetTextWriter()); 
	
			                callback.ConnectionFailure(new ConnectionEventArgs(new EndPoint(addr, port), conn.getRemoteUID()));
			            }
			            finally
			            {
			                synchronized (connections)
			                {
			                    getConnections().remove(conn);
			                }
			            }
	
						
					}
				});
	            
	            conn.setThread(runLoop);
	            
	            runLoop.setDaemon( true);
	            runLoop.setName( getDescription() + " Client thread " + addr.toString());
	            runLoop.start();
        	}
        	catch(IOException ex)
        	{
        		return false;
        	}
        	
          

            return true;
        }




        private void WriteDebug(String msg)
        {
            if (debugBuffer != null)
            {
                synchronized (debugBuffer)
                {
                    debugBuffer.Log(LogInstructions.INFO, msg, true);
                }
            }
        }


        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public int onBroadcastToWire(SendBroadcastMemory msg)
        {
            List<DataConnection> copyConn;

            synchronized (connections)
            {
                copyConn =new Vector<DataConnection>(this.connections);
            }

            // this list assumes that a link  is connected more than onece, 
            // but message is only sent on one of the links
            ListInt sentTo = new ListInt(copyConn.size());
            int broadcasts = 0;

            for(DataConnection c : copyConn)
            {
                if (c.getRemoteUID() != this._localUID &&            // don't send to myself
                    !msg.PeerList.contains(c.getRemoteUID()) &&       // don't send to previous sender
                    !c.isWebClientConnected() &&                  // don't send to web browser
                    !sentTo.contains(c.getRemoteUID()))              // don't send twice
                {
                    //WriteDebug(this.system_id + " pushes a data packet to " + c.RemoteUID);
                    sentTo.add(c.getRemoteUID());
                    c.SendToRemoteClient(msg);
                    broadcasts++;
                }
            }

            //System.Diagnostics.Debug.Assert(broadcasts>0);

            return broadcasts;
            
        }

        /**
         * Used in conjunction with UIDGenerator.GetNextInteger, picks first instance of matchNumber == num[i] for some i
         * @param num array to check
         * @param matchNumber number to match
         * @return number value if succeeded; -1 if failed
         */
    	private int SelectMatch(int[] num, int matchNumber)
    	{
    		for (int i = 0; i < num.length; i++)
    		{
    			if (num[i] == matchNumber)
    				return num[i];
    		}
    		
    		return -1;
    	}
        
        // pick up to any random N number of connections and return them to the caller
        // guaranteed to return at least one connection
        private List<DataConnection> RandomPickConnections(int numConnections)
        {
            List<DataConnection> returns = new Vector<DataConnection>(numConnections);

            // do not lock connections here
            int[] drawNumbers = new int[numConnections];
            int cnt = connections.size();

            for (int i = 0; i < Math.min( numConnections, cnt) ; i++)
            {
                drawNumbers[i] = UIDGenerator.GetNextInteger(cnt);
                // FIXME: no idea what the next line means ???
                if ( i == 0 || SelectMatch(drawNumbers, drawNumbers[i]) == i) // first use of number
                {
                    // add it to the return pool
                    returns.add(connections.get(i));
                }
            }

            return returns;
        }
        
        boolean connectionsHaveRequestForKey(String key)
        {
        	for (DataConnection c : connections)
        	{
        		if (c.HasRequest(key))
        			return true;
        	}
        	
        	return false;
        }
        
        List<DataConnection> connectionsWithKeyRequest(String key)
        {
        	Vector<DataConnection> conn = new Vector<DataConnection>(connections.size());
        	for (DataConnection c : connections)
        	{
        		if (c.HasRequest(key))
        			conn.add(c);
        	}
        	
        	return conn;
        }
        
        /**
         * Returns a DataConnection where
         * x => header.sentFrom.Contains(x.RemoteUID) && x.RemoteUID != LocalID && x.IsConnected 
         */
        DataConnection connectionMatching(ListInt listToTest, int doesNotContainID)
        {
        	for (DataConnection c : connections)
        	{
        		if (c.isConnected() && listToTest.contains(c.getRemoteUID()) && c.getRemoteUID() != doesNotContainID)
        			return c;
        	}
        	
        	return null;
        }

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public int onPullFromPeer(DataHeader header)
        {
            int sentNum = 0;
            synchronized (connections)
            {
                // if no node has made the request, then make a request out to the wire
                // TODO: maybe i should send out a few requests just in case one of them fails
                if (!connectionsHaveRequestForKey(header.key))
                {
                 //   DataConnection thisCon = connections.FirstOrDefault(x => header.sentFrom.contains(x.RemoteUID) && x.RemoteUID != LocalID && x.IsConnected);
                	DataConnection thisCon = connectionMatching(header.sentFrom, getLocalID());
                    if (thisCon != null)
                    {
                        // ask the sender to give the data
                        //WriteDebug(this.system_id + " requests GET from " + thisCon.RemoteUID);
                        thisCon.AddRequest(header);
                        sentNum++;
                    }
                    else
                    {
                        if (connections.size() > 0)
                        {
                            // make simultaneous requests on different links for an answer
                            for (DataConnection backupConn : RandomPickConnections(SIMULATENOUS_REQUESTS))
                            {
                                //WriteDebug(this.system_id + " broadcast requests GET from " + backupConn.RemoteUID);
                                backupConn.AddRequest(header);
                                sentNum++;
                            }
                        }
                        else
                        {
                            WriteDebug(this._localUID + " pullFromPeer because no connections are open");
                        }
                    }
                }
                else
                {
                	// TODO: fix C# code of the same
                	
                    // the data has already been requested on any one of the links, just need to wait
                    // but let's check to see if the link is dead
                    List<DataConnection> checkCons = connectionsWithKeyRequest(header.key);
                    for (DataConnection checkCon : checkCons)
                    {
	                    if (!checkCon.isConnected())
	                    {
	                        // remove dead connection and try again
	                        connections.remove(checkCon);
	                    }
	                    else  
	                    {
	                        // check version
	                        if (checkCon.RemoveOldRequest(header))
	                        {
	                            // make another request from the new data source
	                            sentNum += onPullFromPeer(header);
	                        }
	                        else
	                        {
	                            // okay, request has already been made and it is the same request
	                            sentNum += 1;
	                        }
	                    }
                    }
                    if (sentNum == 0)
                    {
                        sentNum+=onPullFromPeer(header);
                    }
                }

                if (sentNum == 0)
                {
                    WriteDebug("failed to pull from a peer");
                }
            }

           

            return sentNum;
        }


        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public int onPullFromPeer(List<DataHeader> headers)
        {
            int i = 0;
            for (DataHeader h : headers)
            {
                i+=onPullFromPeer(h);
            }
            return i;
        }



        /**
         * Should not be called by client. Will be removed in a future release.
         */
    	public int onSendToPeer(SendMemoryToPeer message)
        {
            DataConnection thisCon;
            synchronized (connections)
            {
            	thisCon = connectionMatching(message.PeerList, getLocalID());
                //thisCon = connections.FirstOrDefault(x => message.PeerList.Contains(x.RemoteUID) && x.RemoteUID != LocalID);
            }

            if (thisCon != null)
            {
                thisCon.SendToRemoteClient(message);
                return 1;
            }
            else
            {
                WriteDebug(this._localUID + " sendToPeer could not find a peer to respond to "+ message.ContentLocation + " in " + message.PeerList.toString() );
                //System.Diagnostics.Debug.Assert(false);
            }
            

            //System.Diagnostics.Debug.Assert(false);

            return 0;
        }


    	/**
    	 * Should not be called by client. Will be removed in a future release.
    	 */
        public int onSendToPeer(List<SendMemoryToPeer> message)
        {
            int i = 0;
            for (SendMemoryToPeer mem : message)
            {
                i+=onSendToPeer(mem);
            }
            return i;
        }

        // Subscriptions


        /// <summary>
        /// adds a subscription that matches the pattern. Pattern matching is Visual Basic patterns (* for many characters, ? for a single character) 
        /// </summary>
        /// <param name="wildcardString">Case-sensitive string that includes *, ?, and [] for ranges of characters to match.</param>
        /**
         *  adds a subscription that matches the pattern.
         * @param regularExpression
         */
        void addSubscription(String regularExpression)
        {
            subscription.AddSubscription(GetFullKey(DATA_NAMESPACE, _partition, regularExpression), SubscriptionInitiator.Manual);
        }


        /// <summary>
        /// adds a subscription that matches the pattern. Pattern matching is Visual Basic patterns (* for many characters, ? for a single character) 
        /// </summary>
        /// <param name="wildcardString">Case-sensitive string that includes *, ?, and [] for ranges of characters to match.</param>
        /**
         * Adds a subscription that matches the key pattern.
         * @param regularExpression
         * @param initiator
         */
        void AddSubscription(String regularExpression, SubscriptionInitiator initiator)
        {
            subscription.AddSubscription(GetFullKeyBypass(DATA_NAMESPACE, _partition, regularExpression), initiator);
        }

        /// <summary>
        /// removes a previously added subscription -- not tested
        /// </summary>
        /// <param name="wildcardKey">The exact string that was added to the subscription.</param>
        /**
         * removes a previously subscription that matches the pattern.
         * @param regularExpression The exact string that was added to the subscription.
         */
        public void removeSubscription(String regularExpression)
        {
            subscription.RemoveSubscription(GetFullKeyBypass(DATA_NAMESPACE, _partition,regularExpression));
        }

        /// <summary>
        /// returns a list of subscriptions
        /// </summary>
        /// <returns></returns>
        public Subscription getSubscriptions()
        {
            return subscription;
        }

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onAddedSubscription(Subscription s, String wildcardString, SubscriptionInitiator initiator)
        {
            synchronized(data)
            {
                for (DataEntry item : data.values())
                {
                    item.subscribed = s.isSubscribed(item.key);
                }
            }

            // raise events as necessary
            SubscriptionEventArgs args = new SubscriptionEventArgs();
            args.SubscripitonPattern = wildcardString;
            args.Reason = SubscriptionEventReason.Add;
            args.Initiator = initiator;
            callback.SubscriptionChanged(args);
        }

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onRemovedSubscription(Subscription s, String wildcardString)
        {
            dataLock.readLock().lock();
            try
            {
                for (DataEntry item : data.values())
                {
                    item.subscribed = s.isSubscribed(item.key);
                }
            }
            finally
            {
                dataLock.readLock().unlock();
            }

            // raise events as necessary
            SubscriptionEventArgs args = new SubscriptionEventArgs();
            args.SubscripitonPattern = wildcardString;
            args.Reason = SubscriptionEventReason.Remove;
            callback.SubscriptionChanged(args);
            
        }


        private boolean connectionHasRemoteID(int id)
        {
        	for(DataConnection c :connections)
        	{
        		if (c.getRemoteUID() == id)
        			return true;
        	}
        	
        	return false;
        }
        
        private  boolean connectionDoNotHaveRemoteID(int id)
        {
        	for(DataConnection c :connections)
        	{
        		if (c.getRemoteUID() != id)
        			return true;
        	}
        	
        	return false;
        }
        
        /**
         * Searches for peers on the network using Apple Bonjour
         * @return true if the network is constructed
         */
        public boolean ConstructNetwork()
        {
            
            ListInt keys ;
            synchronized (PeerDiscovery.getDiscoveredPeers())
            {
                keys = new ListInt(PeerDiscovery.getDiscoveredPeers().keys());
            }

            keys.remove(getLocalID());
            if (keys.size() == 0)
            {
                // BUG: not all peers know about the other peers in the network
                return false;
            }

            int nextUID = keys.getNextIntegerGreaterThan(getLocalID());
            if (nextUID == 0)
            {
                nextUID = keys.get(0);
            }
            

            boolean hasNextConnection = connectionHasRemoteID(nextUID); //this.connections.Exists(x => x.RemoteUID == nextUID);
            boolean hasAnyConnection = connectionDoNotHaveRemoteID(nextUID); //this.connections.Exists(x => x.RemoteUID != nextUID);

            // the next available peer is not connected
            if (!hasNextConnection)
            {
                // remove/re-categorize the old next available peer
                if (constructNwNextPeer != 0)
                {
                    if (constructNwRandomPeer == 0)
                    {
                        constructNwRandomPeer = constructNwNextPeer;
                    }
                    else if (constructNwNextPeer != constructNwRandomPeer)
                    {
                        // too many connections, disconnect but not the random peer
                        for (DataConnection c : connections)
                        {
                        	if(c.getRemoteUID() == constructNwNextPeer && c.isClientConnection() == ConnectionType.Client)
                        	{
                        		c.Close();
                        	}
                        }
                        
                    }
                }
                List<EndpointInfo> nextConnInfo;
                synchronized (PeerDiscovery.getDiscoveredPeers())
                {
                    nextConnInfo = PeerDiscovery.getDiscoveredPeers().get(nextUID);
                }
                synchronized (nextConnInfo)
                {
                    this.OpenClient(nextConnInfo.get(0).Address, nextConnInfo.get(0).Port);
                }
                constructNwNextPeer = nextUID;
            }
            else
            {
                constructNwNextPeer = nextUID;
            }
            
            // only pick another connection if there is something other than the next UID to choose from
            if (!hasAnyConnection &&  keys.size() > 1)
            {
                int pickNum = UIDGenerator.GetNextInteger(keys.size());
                while (keys.get(pickNum) == nextUID )
                {
                    pickNum = UIDGenerator.GetNextInteger(keys.size());
                }

                List<EndpointInfo> nextConnInfo;
                synchronized (PeerDiscovery.getDiscoveredPeers())
                {
                    nextConnInfo = PeerDiscovery.getDiscoveredPeers().get(keys.get(pickNum));
                }
                synchronized (nextConnInfo)
                {
                    this.OpenClient(nextConnInfo.get(0).Address, nextConnInfo.get(0).Port);
                }
                constructNwRandomPeer = keys.get(pickNum);
            }
            

            return true;
        }

        private void ConstructSenderThreads()
        {
            this.senderThreads = new Thread[SENDER_THREADS];
            
            for (int i = 0; i < SENDER_THREADS; i++)
            {
            	final int j = i;
            	
                this.senderThreads[i] = new Thread(new Runnable() {
					
					@Override
					public void run() {
						int offset = j;
			            boolean stuff = false;
			            while (!killbitSenderThreads)
			            {
			                do
			                {
			                    stuff = false;
			                    // only write one thing at a time
			                    for (int i = offset; i < connections.size(); i = i + SENDER_THREADS)
			                    {
			                        DataConnection c =null;
			                        // make this an atomic check
			                        synchronized (connections)
			                        {
			                            if ( i < connections.size())
			                                c = connections.get(i);
			                        }
			                        if (c != null)
			                        {
			                            stuff = c.HandleWrite() || stuff;
			                        }
			                    }
			                } while (stuff);
			                // wait until something needs to be written
			                
			                try
			                {
			                	Thread.sleep(P2PDictionary.SLEEP_IDLE_SLEEP);
			                }
			                catch(InterruptedException ex)
			                {
			                	break;
			                }
			            }
					}
				}, this._description + " sender thread " + i);
                
                this.senderThreads[i].setDaemon(true);
                this.senderThreads[i].start();
            }
        }


        // Dictionary Interface


        private Object m_Tag;

        /**
         * User-defined tag. Not used by the application
         * @return
         */
        public Object getTag()
        {
        	return m_Tag;
        }
        
        /**
         * User-defined tag. Not used by the application
         * @param tag
         */
        public void setTag(Object tag)
        {
        	m_Tag = tag;
        }

        /// <summary>
        /// Searches for the next highest free port starting at basePort.
        /// Throws ApplicationException if port not found.
        /// </summary>
        /// <param name="basePort">valid port number</param>
        /// <returns>free port number</returns>
        public static int findFreePort(int basePort)
        {
            return NetworkUtil.freePort(basePort);
        }

        
        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onNotified(NotificationEventArgs args)
        {
                try
                {
                    NotificationEventArgs newarg = new NotificationEventArgs(args._Entry, GetUserKey(DATA_NAMESPACE, _partition, args.getKey() ), args.getReason(), args.getValue());
                    callback.Notification(newarg);
                }
                catch(Exception ex)
                {
                    WriteDebug("Notified callback raised exception: " + ex);
                }
        }
        

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onSubscriptionChanged(SubscriptionEventArgs args)
        {
            // raise events as necessary
                try
                {
                    SubscriptionEventArgs newarg = new SubscriptionEventArgs();
                    newarg.SubscripitonPattern = args.SubscripitonPattern;
                    newarg.Reason = args.Reason;
                    callback.SubscriptionChanged(newarg);
                }
                catch (Exception ex)
                {
                	WriteDebug("SubscriptionChanged callback raised exception: " + ex);
                }

        }


        public List<EndPointMetadata> getActiveEndPoints()
        {
            
            List<EndPointMetadata> list = new Vector<EndPointMetadata>(connections.size());
            synchronized (this.connections)
            {
                for (DataConnection conn : this.connections)
                {
                    list.add(new EndPointMetadata(conn.getRemoteEndPoint(), conn.getRemoteUID(),conn.isClientConnection()== ConnectionType.Server));
                }
            }
            return list;
            
        }


        /**
         * Returns true if the dictionary with uniqueID is connected directly to this dictionary.
         * Should not be called by client. Will be removed in a future release.
         */
        public boolean isConnected(int uniqueID)
        {
            if (uniqueID == 0)
                return false;

            if (uniqueID == _localUID)
                return true;

            synchronized (this.connections)
            {
            	for (DataConnection c :  this.connections)
            	{
            		//return this.connections.Exists(x => x.RemoteUID == uniqueID);
            		if (c.getRemoteUID()  == uniqueID)
            			return true;
            	}
            }
            
            return false;
        }

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onConnected(DataConnection conn)
        {
           
                try
                {
                    ConnectionEventArgs args = new ConnectionEventArgs(conn.getRemoteEndPoint(), conn.getRemoteUID());
                    callback.Connected(args);
                }
                catch (Exception ex)
                {
                	WriteDebug("Connected callback raised exception: " + ex);
                }
            
        }

        /**
         * Should not be called by client. Will be removed in a future release.
         */
        public void onDisconnected(DataConnection conn)
        {
            
                try
                {
                    ConnectionEventArgs args = new ConnectionEventArgs(conn.getRemoteEndPoint(), conn.getRemoteUID());
                    callback.Disconnected(args);
                }
                catch (Exception ex)
                {
                	WriteDebug("Disconnected callback raised exception: " + ex);
                }
            
        }

		/**
		 * Returns a list of all known endpoints.
		 */
        public List<EndpointInfo> getAllEndPoints()
        {
            if (discovery != null)
            {
                List<EndpointInfo> list = new Vector<EndpointInfo>(PeerDiscovery.getDiscoveredPeers().size());
                for(List<EndpointInfo> l : PeerDiscovery.getDiscoveredPeers().values())
                {
                    for(EndpointInfo m : l)
                    {
                        list.add(m);
                    }
                }

                return list;
            }
            else
            {
                return null;
            }
        }


		@Override
		public boolean containsKey(Object key) {
			if (key instanceof String)
				return containsKey((String) key);
			else
				return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public Object get(Object key) {
			try
			{
				if (key instanceof String)
					return get((String) key);
				else
					return null;
			}
			catch(SubscriptionException ex)
			{
				return null;
			}
		}

		

		@Override
		public Object remove(Object key) {
			// TODO Auto-generated method stub
			if (key instanceof String)
				return remove((String) key);
			else
				return null;
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m)
		{
			for (String s : m.keySet())
			{
				put(s, m.get(s));
			}
		}

		@Override
		public Set<String> keySet() {
			// TODO Auto-generated method stub
            dataLock.readLock().lock();
            Set<String> retValue = new TreeSet<String>();
            try
            {
                for ( String k : this.data.keySet())
                {
                	//this.data.Where(x => IsFullKeyInNamespace(DATA_NAMESPACE, _namespace, x.Key)).Select(x => GetUserKey(DATA_NAMESPACE, _namespace, x.Key)));
                	if (IsFullKeyInNamespace(DATA_NAMESPACE, _partition, k))
                		retValue.add( GetUserKey(DATA_NAMESPACE, _partition, k));
                }
                
            }
            finally
            {
                dataLock.readLock().unlock();
            }
            return retValue;
		}

		@Override
		public Collection<Object> values() {
//          List<KeyValuePair<string, object>>.Enumerator retValue;
//          dataLock.EnterReadLock();
//          try
//          {
//              retValue = new List<KeyValuePair<string, object>>(this.data.Where(x => IsFullKeyInNamespace(DATA_NAMESPACE, _namespace, x.Key)).Select(x => new KeyValuePair<string, object>(GetUserKey(DATA_NAMESPACE, _namespace, x.Key), x.Value.value))).GetEnumerator();
//          }
//          finally
//          {
//              dataLock.ExitReadLock();
//          }
//          return retValue;
			dataLock.readLock().lock();
			Collection<Object> val = new Vector<Object>(data.size());
			
			
			try
			{
				for(String k : data.keySet())
				{
					if (IsFullKeyInNamespace(DATA_NAMESPACE, _partition, k))
					{
						val.add(data.get(k));
					}
				}
			}
			finally
			{
				dataLock.readLock().unlock();
			}

			return val;
		}

		@Override
		public Set<java.util.Map.Entry<String, Object>> entrySet() {
            dataLock.writeLock().lock();
            Set<java.util.Map.Entry<String, Object>> retValue = new TreeSet<java.util.Map.Entry<String, Object>>();
            try
            {
                for ( Entry<String, DataEntry> k : this.data.entrySet())
                {
                	//this.data.Where(x => IsFullKeyInNamespace(DATA_NAMESPACE, _namespace, x.Key)).Select(x => GetUserKey(DATA_NAMESPACE, _namespace, x.Key)));
                	if (IsFullKeyInNamespace(DATA_NAMESPACE, _partition, k.getKey()))
                	{
                		final String key = GetUserKey(DATA_NAMESPACE, _partition, k.getKey());
                		final Object value = k.getValue().value;
                		java.util.Map.Entry<String, Object> entry = new java.util.Map.Entry<String, Object>() {
                			@Override
                			public Object setValue( Object value) {
                				return value;
                			}
                			
                			@Override
                			public String getKey() {
                				return key;
                			}
                			
                			@Override
                			public Object getValue() {
                				return value;
                			}
						}; 
                		retValue.add(entry);
                	}
                }
                
            }
            finally
            {
                dataLock.writeLock().unlock();
            }
            return retValue;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return size() == 0;
		}

		@Override
		public String getFullKey(String userKey) {
			// TODO Auto-generated method stub
			return P2PDictionary.GetFullKey(DATA_NAMESPACE, this._partition, userKey);
		}
}

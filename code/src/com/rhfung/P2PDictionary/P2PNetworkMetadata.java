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
import java.util.Vector;

import com.rhfung.Interop.EndPoint;
import com.rhfung.Interop.ListInt;


class P2PNetworkMetadata {
    /// <summary>
    /// This class provides information regarding the P2P network topology.
    /// Every call is guaranteed thread-safe, i.e., may block on another thread.
    /// </summary>
        private P2PDictionary m_dict;

        public P2PNetworkMetadata(P2PDictionary dictionary)
        {
            m_dict = dictionary;
        }

        public P2PDictionary getDictionary()
        {
            return m_dict;
        }

        /// <summary>
        /// Returns a count of the number of connected peers
        /// </summary>
        public int getRemotePeersCount()
        {
            // TODO: LOCK
            return m_dict.getConnections().size();
        }

        /// <summary>
        /// Returns a list of the IP addresses of the connected peers.
        /// Not guaranteed to match RemotePeersCount.
        /// </summary>
        /// <returns></returns>
        public List<EndPoint> getRemotePeerEndpoints()
        {
            List<EndPoint> endPoints = new Vector<EndPoint>();
            synchronized(m_dict.getConnections())
            {
                for(DataConnection conn : m_dict.getConnections())
                {
                    endPoints.add(conn.getRemoteEndPoint());
                }
            }

            return endPoints;
        }

        /// <summary>
        /// Returns a list of UIDs of each remotely connected dictionary.
        /// </summary>
        /// <returns></returns>
        public ListInt getRemotePeerUID()
        {
            ListInt endPoints = new ListInt();
            synchronized (m_dict.getConnections())
            {
                for (DataConnection conn : m_dict.getConnections())
                {
                  endPoints.add(conn.getRemoteUID());
                }
            }

            return endPoints;
        }
    }



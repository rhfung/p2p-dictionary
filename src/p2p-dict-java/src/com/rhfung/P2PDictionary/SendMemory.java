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

import com.rhfung.Interop.ListInt;
import com.rhfung.Interop.MemoryStream;

abstract class SendMemory
{
    public String ContentLocation;

    /// <summary>
    /// List of peers
    /// </summary>
    public ListInt PeerList;

    public MemoryStream MemBuffer;

    /// <summary>
    /// 
    /// </summary>
    /// <param name="contentLoc">A unique key provided to the send packet</param>
    /// <param name="senderList">These are the senders that have already seen the "data".</param>
    public SendMemory(String contentLoc, ListInt senderList)
    {
        this.ContentLocation = contentLoc;
        this.PeerList = senderList;
        this.MemBuffer = new MemoryStream();
    }


}

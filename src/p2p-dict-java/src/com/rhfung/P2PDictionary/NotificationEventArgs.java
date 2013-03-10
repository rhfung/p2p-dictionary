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

public class NotificationEventArgs {

	NotificationEventArgs(DataEntry entry, String userKey,
			NotificationReason reason, Object oldValue) {
		this._Entry = entry;
		this._reason = reason;
		this._userKey = userKey;
		this._owner = entry.lastOwnerID;
		this._value = entry.value;
		this._oldValue = oldValue;
	}

	DataEntry _Entry;
	private String _userKey;
	private int _owner;
	private NotificationReason _reason;
	private Object _value;
	private Object _oldValue;

	public String getKey() {
		return _userKey;
	}

	// / <summary>
	// / Null if Reason is Removed.
	// / </summary>
	public Object getValue() {
		return this._value;

	}

	// / <summary>
	// / Null if the entry was deleted before a value was acquired by a peer.
	// / </summary>
	public Object getPreviousValue() {
		return this._oldValue;

	}

	public NotificationReason getReason() {
		return this._reason;

	}

	// / <summary>
	// / UID of the sender
	// / </summary>
	public int getSender() {

		return this._owner;

	}

}

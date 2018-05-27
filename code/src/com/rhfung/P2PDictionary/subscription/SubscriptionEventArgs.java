package com.rhfung.P2PDictionary.subscription;

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

import com.rhfung.P2PDictionary.SubscriptionInitiator;

public class SubscriptionEventArgs {
	private SubscriptionEventReason Reason;
	private SubscriptionInitiator Initiator;
	private String SubscriptionPattern;

	public SubscriptionEventReason getReason() {
		return this.Reason;
	}

	public void setReason(SubscriptionEventReason reason) {
		this.Reason = reason;
	}

	public SubscriptionInitiator getInitiator() {
		return this.Initiator;
	}

	public void setInitiator(SubscriptionInitiator initiator) {
		this.Initiator = initiator;
	}

	public String getSubscriptionPattern() {
		return this.SubscriptionPattern;
	}

	public void setSubscriptionPattern(String subscriptionPattern) {
		this.SubscriptionPattern = subscriptionPattern;
	}
}

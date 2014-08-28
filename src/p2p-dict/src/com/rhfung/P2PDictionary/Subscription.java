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

/**
 * Subscriptions in Java are regular expressions, not VB6 string matches.
 * @author Richard
 *
 */
class Subscription  {
	private List<String> subscriptions;
	private ISubscriptionChanged notifier;

	public Subscription(ISubscriptionChanged notifier) {
		this.subscriptions = new Vector<String>();
		this.notifier = notifier;
	}
	
	public List<String> getSubscriptionList()
	{
		return subscriptions;
	}

	// / <summary>
	// /
	// / </summary>
	// / <param name="wildcardString">Case-sensitive string that includes *, ?,
	// and [] for ranges of characters to match.</param>
	public void AddSubscription(String regularExpression, SubscriptionInitiator initiator)
        {
            synchronized (subscriptions)
            {
                subscriptions.add(regularExpression);
            }
            notifier.onAddedSubscription(this, regularExpression, initiator);
        }

	public void RemoveSubscription(String regularExpression)
        {
            synchronized (subscriptions)
            {
                subscriptions.remove(regularExpression);
            }
            notifier.onRemovedSubscription(this, regularExpression);
        }

	public boolean isSubscribed(String regularExpression)
        {
            synchronized (subscriptions)
            {
            	for (String s : subscriptions)
            	{
            		if (s.matches(regularExpression))
            			return true;
            	}
            }
            
            return false;
        }

}

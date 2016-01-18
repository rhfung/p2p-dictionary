package com.rhfung.P2PDictionary.callback;

import com.rhfung.P2PDictionary.ConnectionEventArgs;
import com.rhfung.P2PDictionary.NotificationEventArgs;
import com.rhfung.P2PDictionary.SubscriptionEventArgs;

/**
 * Created by richard on 1/18/16.
 */
public class DefaultCallback extends IDictionaryCallback {
    @Override
    public void SubscriptionChanged(SubscriptionEventArgs e) {

    }

    @Override
    public void Notification(NotificationEventArgs e) {

    }

    @Override
    public void Connected(ConnectionEventArgs e) {

    }

    @Override
    public void Disconnected(ConnectionEventArgs e) {

    }

    @Override
    public void ConnectionFailure(ConnectionEventArgs e) {

    }
}

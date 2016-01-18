package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;

/**
 * Created by richard on 1/18/16.
 */
public interface PeerInterface {
    void RegisterServer(P2PDictionary dict);

    void UnregisterServer();

    void BrowseServices();
}

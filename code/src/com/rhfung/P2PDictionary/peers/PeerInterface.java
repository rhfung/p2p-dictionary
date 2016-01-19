package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;

/**
 * Created by richard on 1/18/16.
 */
public interface PeerInterface {
    /**
     * Register the given dictionary to the service.
     * @param dict
     */
    void RegisterServer(P2PDictionary dict);

    /**
     * Remove the dictionary from the service.
     */
    void UnregisterServer();

    /**
     * Start looking for neighbouring nodes.
     */
    void BrowseServices();
}

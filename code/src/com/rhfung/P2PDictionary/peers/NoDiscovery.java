package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.logging.LogInstructions;

/**
 * Created by richard on 1/18/16.
 */
public class NoDiscovery implements PeerInterface {
    @Override
    public void ConfigureLogging(LogInstructions logger) {

    }

    @Override
    public void RegisterServer(P2PDictionary dict) {

    }

    @Override
    public void UnregisterServer() {

    }

    @Override
    public void BrowseServices() {

    }
}

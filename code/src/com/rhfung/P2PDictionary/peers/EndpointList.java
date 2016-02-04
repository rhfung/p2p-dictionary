package com.rhfung.P2PDictionary.peers;

import java.net.InetAddress;
import java.util.Vector;

/**
 * Created by richard on 2/3/16.
 */
public class EndpointList extends Vector<EndpointInfo> {
    public boolean containsAddress(InetAddress address) {
        for (EndpointInfo info : this) {
            if (info.Address.equals(address)) {
                return true;
            }
        }
        return false;
    }

    public void removeAddress(InetAddress address) {
        for (EndpointInfo info: this) {
            if (info.Address.equals(address)) {
                this.remove(info);
            }
        }
    }
}

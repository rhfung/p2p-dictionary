package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.logging.LogInstructions;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by richard on 2/3/16.
 */
public class GenericBonjour implements PeerInterface, ServiceListener {

    private JmDNS m_instance;
    private boolean m_isBrowsing;

    public GenericBonjour() {
        try {
            m_instance = JmDNS.create(InetAddress.getLocalHost(), "P2P Dictionary Discovery Service");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ConfigureLogging(LogInstructions logger) {

    }

    @Override
    public void RegisterServer(P2PDictionary dict) {
        HashMap<String, String> txtRecord = new HashMap<String, String>();
        txtRecord.put("uid", Integer.toString(dict.getLocalID()));

        if (m_instance == null)  {
            return;
        }

        try {
            m_instance.registerService(ServiceInfo.create(PeerDiscovery.ZEROCONF_NAME,
                    dict.getDescription(),
                    dict.getLocalEndPoint().getPort(),
                    1, 1,
                    txtRecord));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void UnregisterServer() {
        if (m_instance != null) {
            m_instance.unregisterAllServices();
            if (m_isBrowsing) {
                m_instance.removeServiceListener(PeerDiscovery.ZEROCONF_NAME, this);
                m_isBrowsing = false;
            }
            try {
                m_instance.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            m_instance = null;

        }
    }

    @Override
    public void BrowseServices() {
        if (m_instance == null)  {
            return;
        }
        m_instance.addServiceListener(PeerDiscovery.ZEROCONF_NAME, this);
        m_isBrowsing = true;
        }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {

        String uid = serviceEvent.getInfo().getPropertyString("uid");
        if (canParse(uid)) {
            if (!PeerDiscovery.getDiscoveredPeers().containsKey(getKeyForUID(uid))) {
                PeerDiscovery.getDiscoveredPeers().put(getKeyForUID(uid), new EndpointList());
            }
            EndpointList endpoints = PeerDiscovery.getDiscoveredPeers().get(getKeyForUID(uid));
            InetAddress[] addresses = serviceEvent.getInfo().getInetAddresses();
            for (InetAddress address : addresses) {
                if (!endpoints.containsAddress(address)) {
                    endpoints.add(new EndpointInfo(getKeyForUID(uid), address, serviceEvent.getInfo().getPort()));
                }
            }
        }
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {

        String uid = serviceEvent.getInfo().getPropertyString("uid");
        if (canParse(uid)) {
            EndpointList endpoints = PeerDiscovery.getDiscoveredPeers().get(getKeyForUID(uid));
            if (endpoints != null) {
                InetAddress[] addresses = serviceEvent.getInfo().getInetAddresses();
                for (InetAddress address : addresses) {
                    if (!endpoints.containsAddress(address)) {
                        endpoints.removeAddress(address);
                    }
                }
            }
        }
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        serviceAdded(serviceEvent);
    }

    private boolean canParse(String uid) {
        if(uid != null ) {
            try {
                Integer.parseInt(uid);
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;

    }

    private Integer getKeyForUID(String uid) {
        return Integer.parseInt(uid);
    }
}

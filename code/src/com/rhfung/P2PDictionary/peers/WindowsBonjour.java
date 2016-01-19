package com.rhfung.P2PDictionary.peers;

import com.apple.dnssd.*;
import com.rhfung.P2PDictionary.EndpointInfo;
import com.rhfung.P2PDictionary.P2PDictionary;

import java.net.UnknownHostException;
import java.util.Vector;

/**
 * Discover peers using Apple Bonjour on Windows.
 */
public class WindowsBonjour implements PeerInterface {
    private static final String ZEROCONF_NAME = "_com-rhfung-peer._tcp";

    private volatile boolean killBitDiscovery = false;

    private volatile DNSSDRegistration reg = null;
    private volatile DNSSDService service = null;

    class AppResolveListener implements ResolveListener
    {

        @Override
        public void operationFailed(DNSSDService arg0, int arg1) {
            arg0.stop();
        }

        @Override
        public void serviceResolved(DNSSDService resolver, int flags, int ifIndex,
                                    String fullName, String hostName, int post, TXTRecord txtRecord) {
            // serviceResolved(DNSSDService resolver, int flags, int ifIndex, java.lang.String fullName, java.lang.String hostName, int port, TXTRecord txtRecord)

            // arg3 fullname
            // arg4 hostname
            // arg5 port

            if (txtRecord.contains("uid"))
            {
                String uid = txtRecord.getValueAsString("uid");
                int uidInt = Integer.parseInt(uid);
                synchronized (PeerDiscovery.getDiscoveredPeers()) {


                    if (!PeerDiscovery.getDiscoveredPeers().containsKey(uidInt))
                    {
                        PeerDiscovery.getDiscoveredPeers().put(uidInt, new Vector<EndpointInfo>(0));
                    }
                }

                try {
                    synchronized (PeerDiscovery.getDiscoveredPeers().get(uidInt)) {
                        if (PeerDiscovery.getDiscoveredPeers().get(uidInt).size() < 10)
                        {
                            PeerDiscovery.getDiscoveredPeers().get(uidInt).add(new EndpointInfo(uidInt, java.net.InetAddress.getByName(hostName), post ));
                        }
                    }
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block

                }
            }

            if (killBitDiscovery)
            {
                resolver.stop();
                return;
            }
            else if ((DNSSD.MORE_COMING & flags) != DNSSD.MORE_COMING)
            {
                resolver.stop();
            }

        }
    }

    /**
     * Start browsing Apple Bonjour for the P2P dictionary.
     */
    @Override
    public void BrowseServices()
    {
        if (service !=null)
            service.stop();

        try {
            service = DNSSD.browse(ZEROCONF_NAME, new BrowseListener() {

                @Override
                public void operationFailed(DNSSDService arg0, int arg1) {
                    if (killBitDiscovery)
                        arg0.stop();

                }

                @Override
                public void serviceLost(DNSSDService resolver, int flags, int arg2, String arg3,
                                        String arg4, String arg5) {
                    if (killBitDiscovery)
                        resolver.stop();


                }

                @Override
                public void serviceFound(DNSSDService arg0, int arg1, int arg2,
                                         String arg3, String arg4, String arg5) {
                    //public void serviceFound(DNSSDService browser,
//                    int flags,
//                    int ifIndex,
//                    java.lang.String serviceName,
//                    java.lang.String regType,
//                    java.lang.String domain)
                    if (killBitDiscovery)
                    {
                        arg0.stop();
                        return;
                    }

                    try {
                        DNSSD.resolve(0, arg2, arg3, arg4, arg5, new AppResolveListener());
                    } catch (DNSSDException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        } catch (DNSSDException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Adds record to Bonjour
     * @param dict
     */
    @Override
    public void RegisterServer(P2PDictionary dict)
    {
        TXTRecord record = new TXTRecord();
        record.set("uid", Integer.toString( dict.getLocalID()));

        if (reg != null)
        {
            reg.stop();
        }

        try {
            reg = DNSSD.register(0, 0, "com.rhfung.P2PDictionary " + dict.getDescription(), ZEROCONF_NAME, null, null, dict.getLocalEndPoint().getPort(), record, new RegisterListener() {

                @Override
                public void operationFailed(DNSSDService arg0, int arg1) {
                    if (killBitDiscovery)
                        arg0.stop();
                }

                @Override
                public void serviceRegistered(DNSSDRegistration arg0, int arg1,
                                              String arg2, String arg3, String arg4) {
                    if (killBitDiscovery)
                        arg0.stop();

                }
            } );

        } catch (DNSSDException e) {

            e.printStackTrace();
        }
        catch(RuntimeException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Stop registration and discovery service.
     */
    @Override
    public void UnregisterServer()
    {
        killBitDiscovery = true;

        if (reg != null)
        {
            reg.stop();
            reg=null;
        }

        if (service !=null)
        {
            service.stop();
            service =null;
        }
    }


}
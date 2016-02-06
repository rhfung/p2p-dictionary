package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;
import com.rhfung.logging.LogInstructions;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Created by richard on 2/4/16.
 */
public class HelloDiscovery implements PeerInterface {
    final int BUFFER_LENGTH = 255;
    final int DEST_PORT = 7272;
    final String MCAST_ADDR = "239.8.8.0";
    final String MCAST_ADDR_V6 = "ff05::1:3";

    MulticastSocket socket; // must bind receive side
    Thread transmit;
    Thread receive;

    private LogInstructions m_logger;

    @Override
    public void ConfigureLogging(LogInstructions logger) {
        m_logger = logger;
    }

    @Override
    public void RegisterServer(final P2PDictionary dict) {


        transmit = new Thread(new Runnable() {
            @Override
            public void run() {
                Payload payload = preparePayload(new Payload(),
                        dict.getLocalID(),
                        dict.getLocalEndPoint().getPort(),
                        dict.getLocalEndPoint().getAddress(),
                        dict.getNamespace());

                if (payload == null) {
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.WARN, "Cannot prepare a payload for broadcasting", true);
                    }
                    return;
                }

                byte[] b = payload.getBuffer();
                DatagramPacket dgram;

                try {
                    dgram = new DatagramPacket(b, b.length,
                            InetAddress.getByName(MCAST_ADDR), DEST_PORT);
                } catch (UnknownHostException e) {
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.WARN, "Cannot bind a broadcast port", true);
                    }
                    e.printStackTrace();
                    return;
                }

                if (m_logger != null) {
                    m_logger.Log(LogInstructions.INFO, "Sending " + b.length + " bytes to " +
                            dgram.getAddress() + ':' + dgram.getPort(), true);
                }

                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                } catch (SocketException e) {
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.WARN, "Cannot create a datagram socket", true);
                    }
                    return;
                }
                try {

                    while (true) {
                        try {
                            socket.send(dgram);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        transmit.start();

    }

    @Override
    public void UnregisterServer() {
        if (transmit != null) {
            transmit.interrupt();
            transmit = null;
        }

        if (receive != null) {

            if (socket != null) {
                socket.close();
                socket = null;
            }

            receive.interrupt();
            receive = null;
        }
    }

    @Override
    public void BrowseServices() {

        try {
            socket = new MulticastSocket(DEST_PORT);
        } catch (IOException e) {
            if (m_logger != null) {
                m_logger.Log(LogInstructions.WARN, "Cannot enable service listening", true);
            }
            e.printStackTrace();
            return;
        }
        try {
            socket.joinGroup(InetAddress.getByName(MCAST_ADDR));
        } catch (IOException e) {
            if (m_logger != null) {
                m_logger.Log(LogInstructions.WARN, "Cannot join to multicast", true);
            }
            e.printStackTrace();
            socket.close();
            socket = null;
            return;
        }

        receive = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] b = new byte[BUFFER_LENGTH];
                DatagramPacket dgram = new DatagramPacket(b, b.length);

                if (m_logger != null) {
                    m_logger.Log(LogInstructions.INFO, "Waiting to receive messages", true);
                }

                while (socket != null) {
                    try {
                        socket.receive(dgram); // blocks until a datagram is received
                    } catch(SocketException e) {
                        break;
                    } catch (IOException e) {
                        break;
                    }
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.DEBUG, "Received " + dgram.getLength() +
                                " bytes from " + dgram.getAddress(), true);
                    }

                    Payload message = new Payload(dgram.getData());
                    parsePayload(dgram.getAddress(), message);

                    dgram.setLength(b.length); // must reset length field!

                }

            }
        });

        receive.start();
    }

    private void parsePayload(InetAddress address, Payload message) {
        if (message.isHeaderSet()) {
            int identifier = message.getIdentifier();
            if (identifier != 0 && message.getPort() != 0) {
                if (!PeerDiscovery.getDiscoveredPeers().containsKey(identifier)) {
                    PeerDiscovery.getDiscoveredPeers().put(identifier, new EndpointList());
                }

                EndpointList list = PeerDiscovery.getDiscoveredPeers().get(identifier);
                if (!list.containsAddress(address)) {
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.INFO, "Discovered a new peer UID " + identifier + " at " + address + ":" + message.getPort(), true);
                    }
                    list.add(new EndpointInfo(identifier, address, message.getPort()));
                } else {
                    if (m_logger != null) {
                        m_logger.Log(LogInstructions.INFO, "Peer " + identifier + " is still there", true);
                    }
                }

            }
        }
    }


    private Payload preparePayload(Payload message, int uid, int port, InetAddress address, String namespace) {
        message.setHeader();
        message.setIdentifier(uid);
        try {
            message.setAddress((Inet4Address) address);
        } catch (Exception ex) {
            return null;
        }
        message.setPort(port);
        try {
            message.setNamespace(namespace);
        } catch (Exception e) {
            return null;
        }

        return message;
    }

    private class Payload {
        private byte[] m_bytes;

        public Payload() {
            m_bytes = new byte[255];
        }

        public Payload(byte[] buffer) {
            if (buffer.length >= 255) {
                m_bytes = buffer;
            } else {
                m_bytes = new byte[255];
                for (int i = 0; i < buffer.length; i++) {
                    m_bytes[i] = buffer[i];
                }
            }
        }

        public byte[] getBuffer() {
            return m_bytes;
        }

        private byte[] theIdentifier() {
            byte[] result = "com.rhfung.p2pd".getBytes(StandardCharsets.US_ASCII);
            if (result.length <= 20) {
                return result;
            } else {
                throw new RuntimeException("Identifier must be 20 characters or less");
            }
        }

        public void setHeader() {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes);
            buffer.put(theIdentifier());
        }

        public boolean isHeaderSet() {
            byte[] output = theIdentifier();
            for (int i = 0; i < output.length; i++) {
                if (m_bytes[i] != output[i]) {
                    return false;
                }
            }
            return true;
        }

        public void setIdentifier(int id) {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 20, 5);
            buffer.put((byte) 'U');
            buffer.putInt(id);
        }

        public int getIdentifier() {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 20, 5);
            if (buffer.get() != (byte) 'U') {
                return 0;
            }
            return buffer.getInt();
        }

        public void setAddress(Inet4Address address) {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 30, 5);
            buffer.put((byte) '4');
            buffer.put(address.getAddress());
        }

        public InetAddress getAddress() throws UnknownHostException {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 30, 5);
            if (buffer.get() != (byte) '4')  {
                return null;
            }
            byte[] ipaddr = new byte[4];
            buffer.get(ipaddr);
            return Inet4Address.getByAddress(ipaddr);
        }

        public String getNamespace() {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 50, 255 - 50);
            if (buffer.get() != (byte) 'N') {
                return null;
            }

            // convert from signed to unsigned byte for comparison
            byte capacity = buffer.get();
            int capacityInt = capacity;
            if (capacity < 0) {
                capacityInt = capacity + 256;
            }
            if (capacityInt > 200) {
                return null;
            }

            byte[] namespace = new byte[capacityInt];
            buffer.get(namespace);
            return new String(namespace, StandardCharsets.UTF_8);
        }

        public void setNamespace(String namespace) throws Exception {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 50, 255 - 50);
            buffer.put((byte) 'N');

            byte[] namespaceBytes = namespace.getBytes(StandardCharsets.UTF_8);
            if (namespaceBytes.length > 200) {
                throw new Exception("Namespace must fit within 200 bytes");
            }

            buffer.put((byte) namespaceBytes.length);
            buffer.put(namespaceBytes);
        }

        public void setPort(int port) {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 25, 5);
            buffer.put((byte)'P');
            buffer.putShort((short) port);
        }

        public int getPort() {
            ByteBuffer buffer = ByteBuffer.wrap(m_bytes, 25, 5);
            if (buffer.get() != (byte) 'P') {
                return 0;
            }

            int port = buffer.getShort();
            if (port < 0) {
                port += 65536;
            }
            return port;
        }
    }
}

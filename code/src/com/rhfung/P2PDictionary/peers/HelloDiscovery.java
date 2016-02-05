package com.rhfung.P2PDictionary.peers;

import com.rhfung.P2PDictionary.P2PDictionary;

import java.io.IOException;
import java.net.*;

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

    @Override
    public void RegisterServer(P2PDictionary dict) {


        transmit = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] b = new byte[BUFFER_LENGTH];
                DatagramPacket dgram;

                try {
                    dgram = new DatagramPacket(b, b.length,
                            InetAddress.getByName(MCAST_ADDR), DEST_PORT);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    return;
                }

                System.err.println("Sending " + b.length + " bytes to " +
                        dgram.getAddress() + ':' + dgram.getPort());

                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                } catch (SocketException e) {
                    return;
                }
                try {

                    while (true) {
                        System.err.print(".");
                        try {
                            socket.send(dgram);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(1000);
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
            e.printStackTrace();
            return;
        }
        try {
            socket.joinGroup(InetAddress.getByName(MCAST_ADDR));
        } catch (IOException e) {
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

                System.err.println("waiting to receive");

                while (socket != null) {
                    try {
                        socket.receive(dgram); // blocks until a datagram is received
                    } catch(SocketException e) {
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    System.err.println("Received " + dgram.getLength() +
                            " bytes from " + dgram.getAddress());
                    dgram.setLength(b.length); // must reset length field!
                }

            }
        });

        receive.start();
    }

}

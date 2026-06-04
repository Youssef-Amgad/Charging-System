package com.ocs.server.udp;

import com.ocs.server.charging.SessionManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listens for inbound UDP voice packets from all UEs.
 * Prints each packet and discards traffic from sessions that have ended.
 */
public class UDPHandler implements Runnable {

    private static final int    BUFFER_SIZE = 1024;

    private final int            udpPort;
    private final SessionManager sessionManager;
    private volatile boolean     running = true;

    public UDPHandler(int udpPort, SessionManager sessionManager) {
        this.udpPort        = udpPort;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        System.out.println("[UDP] Listener started on port " + udpPort);

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);   // blocks until a packet arrives

                String payload = new String(packet.getData(), 0, packet.getLength()).trim();

                // Format:  MSISDN:VOICE_SEGMENT_N
                if (payload.contains(":")) {
                    String[] parts   = payload.split(":", 2);
                    String   msisdn  = parts[0];
                    String   segment = parts[1];

                    if (sessionManager.hasActiveSession(msisdn)) {
                        System.out.println("[UDP] Received Voice Segment from MSISDN="
                                + msisdn + " → " + segment);
                    } else {
                        // Session ended (low balance or END_CALL) – drop packet silently
                        System.out.println("[UDP] Dropped packet from inactive MSISDN=" + msisdn);
                    }
                } else {
                    System.out.println("[UDP] Malformed packet: " + payload);
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[UDP] Error: " + e.getMessage());
            }
        }

        System.out.println("[UDP] Listener stopped.");
    }

    public void stop() {
        running = false;
    }
}

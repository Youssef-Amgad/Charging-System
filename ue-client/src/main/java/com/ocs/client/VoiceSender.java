package com.ocs.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates a voice media stream by sending UDP datagrams to the server.
 * Runs on its own thread; stopped via {@link #stop()}.
 *
 * Packet format:  MSISDN:VOICE_SEGMENT_N
 */
public class VoiceSender implements Runnable {

    private static final int SEND_INTERVAL_MS = 2_000; // 2 s between segments

    private final String        serverHost;
    private final int           serverUdpPort;
    private final String        msisdn;
    private final AtomicBoolean running;

    public VoiceSender(String serverHost, int serverUdpPort, String msisdn) {
        this.serverHost    = serverHost;
        this.serverUdpPort = serverUdpPort;
        this.msisdn        = msisdn;
        this.running       = new AtomicBoolean(true);
    }

    @Override
    public void run() {
        System.out.println("[UDP] Voice sender started for MSISDN=" + msisdn);

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddr = InetAddress.getByName(serverHost);
            int segment = 1;

            while (running.get()) {
                String payload  = msisdn + ":VOICE_SEGMENT_" + segment;
                byte[] data     = payload.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        data, data.length, serverAddr, serverUdpPort);

                socket.send(packet);
                System.out.println("[UDP] Sent: " + payload);

                segment++;
                Thread.sleep(SEND_INTERVAL_MS);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (running.get()) {
                System.err.println("[UDP] Error: " + e.getMessage());
            }
        }

        System.out.println("[UDP] Voice sender stopped for MSISDN=" + msisdn);
    }

    /** Signals the sender loop to exit cleanly. */
    public void stop() {
        running.set(false);
    }
}

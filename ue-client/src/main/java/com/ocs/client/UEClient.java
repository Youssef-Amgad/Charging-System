package com.ocs.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User Equipment (UE) application.
 *
 * Usage:
 *   java -jar ue.jar <MSISDN>
 *
 * Example:
 *   java -jar ue.jar 01000202
 *
 * Optional JVM flags:
 *   -Dserver.host=127.0.0.1   (default: 127.0.0.1)
 *   -Dserver.tcp.port=5000    (default: 5000)
 *   -Dserver.udp.port=5001    (default: 5001)
 */
public class UEClient {

    private static final String SERVER_HOST    = System.getProperty("server.host",         "127.0.0.1");
    private static final int    TCP_PORT       = Integer.parseInt(System.getProperty("server.tcp.port", "5000"));
    private static final int    UDP_PORT       = Integer.parseInt(System.getProperty("server.udp.port", "5001"));

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar ue.jar <MSISDN>");
            System.exit(1);
        }

        String msisdn = args[0];

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Simple Online Charging System      ║");
        System.out.printf ("║   UE Client  –  MSISDN: %-13s║%n", msisdn);
        System.out.println("╚══════════════════════════════════════╝");

        new UEClient().run(msisdn);
    }

    private void run(String msisdn) {
        ExecutorService voiceExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "voice-sender");
            t.setDaemon(true);
            return t;
        });
        VoiceSender voiceSender = null;

        try (Socket        tcpSocket = new Socket(SERVER_HOST, TCP_PORT);
             PrintWriter   out       = new PrintWriter(tcpSocket.getOutputStream(), true);
             BufferedReader in       = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()))) {

            System.out.printf("[TCP] Connected to %s:%d%n", SERVER_HOST, TCP_PORT);

            // ── Step 1: Send START_CALL ──────────────────────────────────────
            String startMsg = "START_CALL:" + msisdn;
            out.println(startMsg);
            System.out.println("[TCP] Sent: " + startMsg);

            // ── Step 2: Wait for ANSWERED ────────────────────────────────────
            String response = in.readLine();
            System.out.println("[TCP] Received: " + response);

            if (!"ANSWERED".equals(response)) {
                System.out.println("[UE] Call not accepted by server. Exiting.");
                return;
            }

            System.out.println("[UE] Call is active! Press ENTER to end the call.");

            // ── Step 3: Start UDP voice stream ───────────────────────────────
            voiceSender = new VoiceSender(SERVER_HOST, UDP_PORT, msisdn);
            voiceExecutor.submit(voiceSender);

            // ── Step 4: Listen for server-side termination in background ─────
            final VoiceSender finalVoiceSender = voiceSender;
            Thread serverListener = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println("[TCP] Server: " + serverMsg);
                        if ("CALL_TERMINATED_LOW_BALANCE".equals(serverMsg)) {
                            System.out.println("[UE] Call terminated by server (low balance).");
                            finalVoiceSender.stop();
                            // Interrupt the main thread waiting on Scanner
                            Thread.currentThread().interrupt();
                            System.exit(0);
                        }
                    }
                } catch (IOException e) {
                    // Socket closed – normal on END_CALL
                }
            }, "server-listener");
            serverListener.setDaemon(true);
            serverListener.start();

            // ── Step 5: Wait for user to press ENTER ────────────────────────
            new Scanner(System.in).nextLine();

            // ── Step 6: Send END_CALL ────────────────────────────────────────
            String endMsg = "END_CALL:" + msisdn;
            out.println(endMsg);
            System.out.println("[TCP] Sent: " + endMsg);

        } catch (IOException e) {
            System.err.println("[UE] Error: " + e.getMessage());
        } finally {
            if (voiceSender != null) voiceSender.stop();
            voiceExecutor.shutdownNow();
            System.out.println("[UE] Goodbye.");
        }
    }
}

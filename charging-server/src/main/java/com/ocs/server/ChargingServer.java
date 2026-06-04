package com.ocs.server;

import com.ocs.db.CustomerDAO;
import com.ocs.db.DatabaseConnection;
import com.ocs.server.charging.SessionManager;
import com.ocs.server.tcp.TCPHandler;
import com.ocs.server.udp.UDPHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Charging Server main class.
 *
 * Architecture:
 *   - One TCP ServerSocket accepts UE connections; each is handed to a TCPHandler thread.
 *   - One UDPHandler thread receives all voice packets.
 *   - Each active call spawns its own ScheduledExecutorService (inside TCPHandler).
 *
 * Defaults:
 *   TCP port : 5000  (override with -Dtcp.port=XXXX)
 *   UDP port : 5001  (override with -Dudp.port=XXXX)
 */
public class ChargingServer {

    private static final int TCP_PORT =
            Integer.parseInt(System.getProperty("tcp.port", "5000"));
    private static final int UDP_PORT =
            Integer.parseInt(System.getProperty("udp.port", "5001"));

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   Simple Online Charging System      ║");
        System.out.println("║   Charging Server v1.0               ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Shared dependencies
        SessionManager sessionManager = new SessionManager();
        CustomerDAO    customerDAO    = new CustomerDAO();

        // Thread pool for TCP connections (one thread per active UE)
        ExecutorService tcpPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("tcp-handler-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        // Start UDP listener on its own thread
        UDPHandler udpHandler = new UDPHandler(UDP_PORT, sessionManager);
        Thread udpThread = new Thread(udpHandler, "udp-listener");
        udpThread.setDaemon(true);
        udpThread.start();

        // Shutdown hook for clean teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SERVER] Shutting down...");
            udpHandler.stop();
            tcpPool.shutdownNow();
            DatabaseConnection.close();
        }));

        // Accept TCP connections
        System.out.printf("[SERVER] TCP listening on port %d%n", TCP_PORT);
        System.out.printf("[SERVER] UDP listening on port %d%n", UDP_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                tcpPool.submit(new TCPHandler(clientSocket, customerDAO, sessionManager));
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
        }
    }
}

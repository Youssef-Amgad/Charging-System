package com.ocs.server.tcp;

import com.ocs.db.CustomerDAO;
import com.ocs.model.CallSession;
import com.ocs.model.Customer;
import com.ocs.server.charging.ChargingTask;
import com.ocs.server.charging.SessionManager;

import java.io.*;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles the TCP lifecycle for one UE:
 *   START_CALL  → validate → ANSWERED → start charging scheduler
 *   END_CALL    → stop charging → clean up
 */
public class TCPHandler implements Runnable {

    // Charging interval: 60 seconds in production; can override for testing
    private static final long CHARGE_INTERVAL_SECONDS =
            Long.parseLong(System.getProperty("charge.interval.seconds", "60"));

    private final Socket         clientSocket;
    private final CustomerDAO    customerDAO;
    private final SessionManager sessionManager;

    // Each call gets its own single-thread scheduler so it can be cancelled independently
    private ScheduledExecutorService chargingScheduler;

    public TCPHandler(Socket clientSocket,
                      CustomerDAO customerDAO,
                      SessionManager sessionManager) {
        this.clientSocket   = clientSocket;
        this.customerDAO    = customerDAO;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        String remoteAddr = clientSocket.getRemoteSocketAddress().toString();
        System.out.println("[TCP] New connection from " + remoteAddr);

        try (BufferedReader  in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter     out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                System.out.println("[TCP] Received: " + line);

                if (line.startsWith("START_CALL:")) {
                    handleStartCall(line, out);

                } else if (line.startsWith("END_CALL:")) {
                    handleEndCall(line);
                    break; // done with this connection

                } else {
                    System.out.println("[TCP] Unknown message: " + line);
                }
            }

        } catch (IOException e) {
            System.err.println("[TCP] Connection error: " + e.getMessage());
        } finally {
            closeSocket();
        }

        System.out.println("[TCP] Connection closed for " + remoteAddr);
    }

    // ── Message handlers ─────────────────────────────────────────────────────

    private void handleStartCall(String message, PrintWriter out) {
        String msisdn = parseMsisdn(message, "START_CALL:");

        System.out.println("[TCP] START_CALL received for MSISDN=" + msisdn);

        // Validate customer in DB
        Optional<Customer> customerOpt = customerDAO.findByMsisdn(msisdn);
        if (customerOpt.isEmpty()) {
            System.out.println("[TCP] REJECTED – MSISDN not found: " + msisdn);
            out.println("REJECTED_UNKNOWN_MSISDN");
            return;
        }

        Customer customer = customerOpt.get();
        if (customer.getBalance().signum() <= 0) {
            System.out.println("[TCP] REJECTED – Zero balance for MSISDN=" + msisdn);
            out.println("REJECTED_INSUFFICIENT_BALANCE");
            return;
        }

        // Create session
        CallSession session = new CallSession(msisdn, clientSocket);
        sessionManager.addSession(msisdn, session);

        // Send ANSWERED
        out.println("ANSWERED");
        System.out.println("[TCP] ANSWERED sent for MSISDN=" + msisdn);

        // Start charging scheduler immediately after ANSWERED
        startChargingScheduler(session);
    }

    private void handleEndCall(String message) {
        String msisdn = parseMsisdn(message, "END_CALL:");
        System.out.println("[TCP] END_CALL received for MSISDN=" + msisdn);

        sessionManager.removeSession(msisdn);
        stopChargingScheduler();
        System.out.println("[SESSION] Session terminated for MSISDN=" + msisdn);
    }

    // ── Charging scheduler ───────────────────────────────────────────────────

    private void startChargingScheduler(CallSession session) {
        chargingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "charging-" + session.getMsisdn());
            t.setDaemon(true);
            return t;
        });

        ChargingTask task = new ChargingTask(session, customerDAO, sessionManager);

        ScheduledFuture<?> future = chargingScheduler.scheduleAtFixedRate(
                task,
                CHARGE_INTERVAL_SECONDS,   // initial delay
                CHARGE_INTERVAL_SECONDS,   // period
                TimeUnit.SECONDS);

        session.setChargingFuture(future);
        System.out.printf("[CHARGING] Scheduler started for MSISDN=%s (every %ds)%n",
                session.getMsisdn(), CHARGE_INTERVAL_SECONDS);
    }

    private void stopChargingScheduler() {
        if (chargingScheduler != null && !chargingScheduler.isShutdown()) {
            chargingScheduler.shutdownNow();
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private String parseMsisdn(String message, String prefix) {
        return message.substring(prefix.length()).trim();
    }

    private void closeSocket() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[TCP] Error closing socket: " + e.getMessage());
        }
    }
}

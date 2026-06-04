package com.ocs.server.charging;

import com.ocs.db.CustomerDAO;
import com.ocs.model.CallSession;
import com.ocs.model.Customer;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Runnable executed every 60 seconds by a ScheduledExecutorService.
 * Deducts 1.00 LE per interval and handles low-balance termination.
 */
public class ChargingTask implements Runnable {

    private static final BigDecimal CHARGE_PER_MINUTE = new BigDecimal("1.00");

    private final CallSession session;
    private final CustomerDAO customerDAO;
    private final SessionManager sessionManager;

    public ChargingTask(CallSession session,
                        CustomerDAO customerDAO,
                        SessionManager sessionManager) {
        this.session        = session;
        this.customerDAO    = customerDAO;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        // Guard: skip if session already ended
        if (session.isTerminated()) return;

        String msisdn = session.getMsisdn();

        Optional<Customer> result = customerDAO.deductBalance(msisdn, CHARGE_PER_MINUTE);

        if (result.isEmpty()) {
            System.err.println("[CHARGING] Could not deduct balance for MSISDN=" + msisdn);
            return;
        }

        Customer updated = result.get();
        BigDecimal balance = updated.getBalance();

        System.out.printf("[CHARGING] MSISDN=%s  Current Balance=%.2f%n", msisdn, balance);

        // Low-balance check
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[CHARGING] Low balance for MSISDN=" + msisdn + " – terminating call.");
            terminateForLowBalance(msisdn);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void terminateForLowBalance(String msisdn) {
        session.setTerminated(true);
        session.cancelCharging();

        // Notify UE over existing TCP connection
        try {
            PrintWriter out = new PrintWriter(session.getTcpSocket().getOutputStream(), true);
            out.println("CALL_TERMINATED_LOW_BALANCE");
            System.out.println("[TCP] Sent CALL_TERMINATED_LOW_BALANCE to MSISDN=" + msisdn);
        } catch (IOException e) {
            System.err.println("[CHARGING] Could not notify UE of low balance: " + e.getMessage());
        }

        sessionManager.removeSession(msisdn);
    }
}

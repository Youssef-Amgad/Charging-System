package com.ocs.model;

import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents one active call session on the server side.
 * Holds the TCP socket, charging task handle, and call metadata.
 */
public class CallSession {

    private final String   msisdn;
    private final Socket   tcpSocket;
    private final Instant  startTime;

    /** Handle returned by ScheduledExecutorService – used to cancel charging. */
    private volatile ScheduledFuture<?> chargingFuture;

    /** Set to true when the session ends (gracefully or due to low balance). */
    private volatile boolean terminated = false;

    public CallSession(String msisdn, Socket tcpSocket) {
        this.msisdn    = msisdn;
        this.tcpSocket = tcpSocket;
        this.startTime = Instant.now();
    }

    // ── Getters / setters ────────────────────────────────────────────────────

    public String  getMsisdn()   { return msisdn;    }
    public Socket  getTcpSocket(){ return tcpSocket;  }
    public Instant getStartTime(){ return startTime;  }

    public ScheduledFuture<?> getChargingFuture()               { return chargingFuture; }
    public void setChargingFuture(ScheduledFuture<?> future)    { this.chargingFuture = future; }

    public boolean isTerminated()       { return terminated; }
    public void    setTerminated(boolean v) { this.terminated = v; }

    /** Cancels the charging scheduler without interrupting a running deduction. */
    public void cancelCharging() {
        if (chargingFuture != null && !chargingFuture.isDone()) {
            chargingFuture.cancel(false);
            System.out.println("[SESSION] Charging scheduler stopped for MSISDN=" + msisdn);
        }
    }

    @Override
    public String toString() {
        return "CallSession{msisdn='" + msisdn + "', start=" + startTime + '}';
    }
}

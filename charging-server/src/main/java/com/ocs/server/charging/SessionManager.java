package com.ocs.server.charging;

import com.ocs.model.CallSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Thread-safe registry for all active CallSessions.
 * Used by TCPHandler to register/unregister sessions and by
 * UDPHandler to check whether a call is still active.
 */
public class SessionManager {

    private final ConcurrentHashMap<String, CallSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String msisdn, CallSession session) {
        sessions.put(msisdn, session);
        System.out.println("[SESSION] Session added for MSISDN=" + msisdn);
    }

    public Optional<CallSession> getSession(String msisdn) {
        return Optional.ofNullable(sessions.get(msisdn));
    }

    public void removeSession(String msisdn) {
        CallSession session = sessions.remove(msisdn);
        if (session != null) {
            session.setTerminated(true);
            session.cancelCharging();
            System.out.println("[SESSION] Session removed for MSISDN=" + msisdn);
        }
    }

    public boolean hasActiveSession(String msisdn) {
        return sessions.containsKey(msisdn) &&
               !sessions.get(msisdn).isTerminated();
    }

    public int activeSessions() {
        return sessions.size();
    }
}

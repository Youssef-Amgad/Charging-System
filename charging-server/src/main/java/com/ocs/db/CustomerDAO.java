package com.ocs.db;

import com.ocs.model.Customer;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for the customers table.
 * All balance mutations use explicit transactions for consistency.
 */
public class CustomerDAO {

    // ── Queries ─────────────────────────────────────────────────────────────
    private static final String SELECT_BY_MSISDN =
            "SELECT msisdn, balance FROM customers WHERE msisdn = ?";

    private static final String UPDATE_BALANCE =
            "UPDATE customers SET balance = ? WHERE msisdn = ?";

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Looks up a customer by MSISDN.
     *
     * @param msisdn the subscriber's phone number
     * @return Optional containing the Customer if found, empty otherwise
     */
    public Optional<Customer> findByMsisdn(String msisdn) {
        try (PreparedStatement ps =
                     DatabaseConnection.getConnection().prepareStatement(SELECT_BY_MSISDN)) {
            ps.setString(1, msisdn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Customer(
                            rs.getString("msisdn"),
                            rs.getBigDecimal("balance")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] findByMsisdn error: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Deducts {@code amount} LE from the customer's balance atomically.
     *
     * @param msisdn the subscriber's phone number
     * @param amount amount to deduct (positive value)
     * @return the updated Customer, or empty if MSISDN not found
     */
    public Optional<Customer> deductBalance(String msisdn, BigDecimal amount) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Read current balance
            Optional<Customer> customerOpt = findByMsisdn(msisdn);
            if (customerOpt.isEmpty()) {
                conn.rollback();
                return Optional.empty();
            }

            Customer customer = customerOpt.get();
            BigDecimal oldBalance = customer.getBalance();
            BigDecimal newBalance = oldBalance.subtract(amount);

            // Write new balance
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_BALANCE)) {
                ps.setBigDecimal(1, newBalance);
                ps.setString(2, msisdn);
                ps.executeUpdate();
            }

            conn.commit();

            Customer updated = new Customer(msisdn, newBalance);

            System.out.printf("[CHARGING] MSISDN=%-15s  Old Balance=%6.2f  New Balance=%6.2f%n",
                    msisdn, oldBalance, newBalance);

            return Optional.of(updated);

        } catch (SQLException e) {
            System.err.println("[DAO] deductBalance error: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
            }
        }
        return Optional.empty();
    }
}

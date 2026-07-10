package dao;

import model.Customer;
import model.DashboardStats;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data access layer for the public.customers table.
 * Every query uses PreparedStatement to prevent SQL injection.
 */
public class CustomerDAO {

    // Whitelist of sortable columns to avoid injecting into ORDER BY.
    private static final Set<String> SORTABLE_COLUMNS = Set.of("msisdn", "balance");

    /**
     * Inserts a new customer. Throws DuplicateMsisdnException if the MSISDN
     * already exists (checked explicitly, and also guarded by the PK constraint).
     */
    public void addCustomer(Customer customer) throws SQLException, DuplicateMsisdnException {
        if (existsByMsisdn(customer.getMsisdn())) {
            throw new DuplicateMsisdnException(customer.getMsisdn());
        }
        String sql = "INSERT INTO public.customers (msisdn, balance) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getMsisdn());
            ps.setBigDecimal(2, customer.getBalance());
            ps.executeUpdate();
        } catch (SQLException e) {
            // 23505 = unique_violation (race condition safety net)
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateMsisdnException(customer.getMsisdn());
            }
            throw e;
        }
    }

    /**
     * Updates only the balance for a given MSISDN (MSISDN itself is immutable / the PK).
     */
    public void updateBalance(String msisdn, BigDecimal newBalance) throws SQLException, CustomerNotFoundException {
        String sql = "UPDATE public.customers SET balance = ? WHERE msisdn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, msisdn);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new CustomerNotFoundException(msisdn);
            }
        }
    }

    /**
     * Deletes a customer by MSISDN.
     */
    public void deleteCustomer(String msisdn) throws SQLException, CustomerNotFoundException {
        String sql = "DELETE FROM public.customers WHERE msisdn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msisdn);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new CustomerNotFoundException(msisdn);
            }
        }
    }

    /**
     * Fetches a single customer by MSISDN, or null if not present.
     */
    public Customer getByMsisdn(String msisdn) throws SQLException {
        String sql = "SELECT msisdn, balance FROM public.customers WHERE msisdn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msisdn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public boolean existsByMsisdn(String msisdn) throws SQLException {
        String sql = "SELECT 1 FROM public.customers WHERE msisdn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msisdn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns a page of customers, optionally filtered by an MSISDN search term,
     * sorted by the given column/direction.
     *
     * @param searchTerm  substring to match against msisdn (nullable / blank = no filter)
     * @param sortColumn  "msisdn" or "balance" (defaults to msisdn if invalid)
     * @param sortDir     "asc" or "desc" (defaults to asc if invalid)
     * @param page        1-based page number
     * @param pageSize    rows per page
     */
    public List<Customer> searchCustomers(String searchTerm, String sortColumn, String sortDir,
                                           int page, int pageSize) throws SQLException {
        String column = SORTABLE_COLUMNS.contains(sortColumn) ? sortColumn : "msisdn";
        String direction = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safeSize;

        boolean hasFilter = searchTerm != null && !searchTerm.isBlank();
        String sql = "SELECT msisdn, balance FROM public.customers "
                + (hasFilter ? "WHERE msisdn ILIKE ? " : "")
                + "ORDER BY " + column + " " + direction + " "
                + "LIMIT ? OFFSET ?";

        List<Customer> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (hasFilter) {
                ps.setString(idx++, "%" + searchTerm + "%");
            }
            ps.setInt(idx++, safeSize);
            ps.setInt(idx, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /**
     * Counts customers matching an optional MSISDN search term (used for pagination totals).
     */
    public long countCustomers(String searchTerm) throws SQLException {
        boolean hasFilter = searchTerm != null && !searchTerm.isBlank();
        String sql = "SELECT COUNT(*) FROM public.customers " + (hasFilter ? "WHERE msisdn ILIKE ?" : "");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasFilter) {
                ps.setString(1, "%" + searchTerm + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Computes the dashboard aggregate statistics in a single round trip.
     */
    public DashboardStats getDashboardStats() throws SQLException {
        String sql = "SELECT COUNT(*) AS total, "
                + "COALESCE(SUM(balance), 0) AS total_balance, "
                + "COALESCE(AVG(balance), 0) AS avg_balance, "
                + "COALESCE(MAX(balance), 0) AS max_balance, "
                + "COALESCE(MIN(balance), 0) AS min_balance "
                + "FROM public.customers";

        DashboardStats stats = new DashboardStats();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setTotalCustomers(rs.getLong("total"));
                stats.setTotalBalance(rs.getBigDecimal("total_balance"));
                stats.setAverageBalance(rs.getBigDecimal("avg_balance").setScale(2, java.math.RoundingMode.HALF_UP));
                stats.setHighestBalance(rs.getBigDecimal("max_balance"));
                stats.setLowestBalance(rs.getBigDecimal("min_balance"));
            }
        }
        return stats;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setMsisdn(rs.getString("msisdn"));
        c.setBalance(rs.getBigDecimal("balance"));
        return c;
    }
}

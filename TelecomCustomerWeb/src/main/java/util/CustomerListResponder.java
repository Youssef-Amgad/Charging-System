package util;

import dao.CustomerDAO;
import model.Customer;

import java.util.List;

/**
 * Shared logic for turning request parameters into a JSON page of customers.
 * Reused by CustomerListServlet (full table browsing) and SearchCustomerServlet
 * (live search box) so both stay consistent.
 */
public final class CustomerListResponder {

    private CustomerListResponder() {
    }

    public static String buildJson(CustomerDAO dao, String search, String sortColumn, String sortDir,
                                    int page, int pageSize) throws java.sql.SQLException {
        List<Customer> customers = dao.searchCustomers(search, sortColumn, sortDir, page, pageSize);
        long total = dao.countCustomers(search);
        long totalPages = (long) Math.ceil(total / (double) pageSize);
        if (totalPages == 0) totalPages = 1;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"page\":").append(page).append(",");
        sb.append("\"pageSize\":").append(pageSize).append(",");
        sb.append("\"totalRecords\":").append(total).append(",");
        sb.append("\"totalPages\":").append(totalPages).append(",");
        sb.append("\"customers\":[");
        for (int i = 0; i < customers.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(customers.get(i).toJson());
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }
}

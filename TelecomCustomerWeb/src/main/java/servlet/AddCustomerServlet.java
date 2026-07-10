package servlet;

import dao.CustomerDAO;
import dao.DuplicateMsisdnException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Customer;
import util.JsonUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Handles creation of a new customer row. Expects form-encoded POST body:
 * msisdn, balance
 */
@WebServlet(name = "AddCustomerServlet", urlPatterns = {"/api/customers/add"})
public class AddCustomerServlet extends HttpServlet {

    // Accepts 10-20 digits, optionally prefixed with '+'
    private static final Pattern MSISDN_PATTERN = Pattern.compile("^\\+?\\d{10,20}$");

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String msisdn = trim(request.getParameter("msisdn"));
        String balanceRaw = trim(request.getParameter("balance"));

        if (msisdn == null || msisdn.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("MSISDN is required."));
            return;
        }
        if (!MSISDN_PATTERN.matcher(msisdn).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("Invalid MSISDN format. Use 10-20 digits, optionally prefixed with '+'."));
            return;
        }
        if (balanceRaw == null || balanceRaw.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("Balance is required."));
            return;
        }

        BigDecimal balance;
        try {
            balance = new BigDecimal(balanceRaw);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("Invalid balance. Please enter a valid number."));
            return;
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("Balance cannot be negative."));
            return;
        }

        try {
            Customer customer = new Customer(msisdn, balance.setScale(2, java.math.RoundingMode.HALF_UP));
            customerDAO.addCustomer(customer);
            out.write(JsonUtil.successMessage("Customer " + msisdn + " added successfully."));
        } catch (DuplicateMsisdnException e) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.write(JsonUtil.errorMessage(e.getMessage()));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(JsonUtil.errorMessage("Database connection failed: " + e.getMessage()));
        }
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }
}

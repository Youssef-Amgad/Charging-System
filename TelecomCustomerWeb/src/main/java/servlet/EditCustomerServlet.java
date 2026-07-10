package servlet;

import dao.CustomerDAO;
import dao.CustomerNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.JsonUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Updates the balance of an existing customer. MSISDN is immutable (the PK)
 * so only "balance" may be changed. Expects form-encoded POST body:
 * msisdn, balance
 */
@WebServlet(name = "EditCustomerServlet", urlPatterns = {"/api/customers/edit"})
public class EditCustomerServlet extends HttpServlet {

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
            customerDAO.updateBalance(msisdn, balance.setScale(2, java.math.RoundingMode.HALF_UP));
            out.write(JsonUtil.successMessage("Customer " + msisdn + " updated successfully."));
        } catch (CustomerNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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

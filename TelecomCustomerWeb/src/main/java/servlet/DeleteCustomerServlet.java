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
import java.sql.SQLException;

/**
 * Deletes a customer by MSISDN. Expects form-encoded POST body: msisdn
 * (the confirmation dialog is handled client-side in JavaScript before this is called).
 */
@WebServlet(name = "DeleteCustomerServlet", urlPatterns = {"/api/customers/delete"})
public class DeleteCustomerServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String msisdn = request.getParameter("msisdn");
        if (msisdn == null || msisdn.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(JsonUtil.errorMessage("MSISDN is required."));
            return;
        }
        msisdn = msisdn.trim();

        try {
            customerDAO.deleteCustomer(msisdn);
            out.write(JsonUtil.successMessage("Customer " + msisdn + " deleted successfully."));
        } catch (CustomerNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write(JsonUtil.errorMessage(e.getMessage()));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(JsonUtil.errorMessage("Database connection failed: " + e.getMessage()));
        }
    }
}

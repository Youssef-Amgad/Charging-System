package servlet;

import dao.CustomerDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.CustomerListResponder;
import util.JsonUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Dedicated endpoint backing the live search box: filters customers by MSISDN
 * substring without a full page reload. Query params: q, sort, dir, page, size.
 */
@WebServlet(name = "SearchCustomerServlet", urlPatterns = {"/api/customers/search"})
public class SearchCustomerServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String search = request.getParameter("q");
        String sort = request.getParameter("sort");
        String dir = request.getParameter("dir");
        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int size = parseIntOrDefault(request.getParameter("size"), 10);

        try (PrintWriter out = response.getWriter()) {
            String json = CustomerListResponder.buildJson(customerDAO, search, sort, dir, page, size);
            out.write(json);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(JsonUtil.errorMessage("Database connection failed: " + e.getMessage()));
            }
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

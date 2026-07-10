package servlet;

import dao.CustomerDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.DashboardStats;
import util.JsonUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Serves dashboard aggregate statistics (total customers, total/avg/max/min balance)
 * as JSON, consumed by the dashboard cards on the home page.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/api/dashboard"})
public class HomeServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            DashboardStats stats = customerDAO.getDashboardStats();
            out.write(stats.toJson());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.write(JsonUtil.errorMessage("Database connection failed: " + e.getMessage()));
            }
        }
    }
}

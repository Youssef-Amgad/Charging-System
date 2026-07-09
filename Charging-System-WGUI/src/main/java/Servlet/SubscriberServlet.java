package Servlet;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/SubscriberServlet")
public class SubscriberServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
    // Database connection from environment variables
    private static final String DB_URL = System.getenv("DB_URL") != null 
        ? System.getenv("DB_URL") 
        : "jdbc:postgresql://localhost/neondb";
    private static final String DB_USER = System.getenv("DB_USER") != null 
        ? System.getenv("DB_USER") 
        : "postgres";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null 
        ? System.getenv("DB_PASSWORD") 
        : "";
    
    // Get PostgreSQL connection
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL Driver not found", e);
        }
    }
    
    // Add CORS headers
    private void addCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
    
    // Handle CORS preflight requests
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    // GET - Get all subscribers or single subscriber
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        addCORSHeaders(response);
        
        String msisdn = request.getParameter("msisdn");
        
        try (Connection conn = getConnection()) {
            if (msisdn != null && !msisdn.isEmpty()) {
                // Get single subscriber
                String sql = "SELECT msisdn, balance FROM customers WHERE msisdn = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, msisdn);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> subscriber = new HashMap<>();
                            subscriber.put("msisdn", rs.getString("msisdn"));
                            subscriber.put("balance", rs.getDouble("balance"));
                            String jsonResponse = gson.toJson(subscriber);
                            response.getWriter().write(jsonResponse);
                        } else {
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            response.getWriter().write("null");
                        }
                    }
                }
            } else {
                // Get all subscribers
                String sql = "SELECT msisdn, balance FROM customers ORDER BY msisdn";
                List<Map<String, Object>> subscribers = new ArrayList<>();
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        Map<String, Object> subscriber = new HashMap<>();
                        subscriber.put("msisdn", rs.getString("msisdn"));
                        subscriber.put("balance", rs.getDouble("balance"));
                        subscribers.add(subscriber);
                    }
                }
                String jsonResponse = gson.toJson(subscribers);
                response.getWriter().write(jsonResponse);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        }
    }
    
    // POST - Add new subscriber
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        addCORSHeaders(response);
        
        try {
            // Parse JSON request body
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String action = json.get("action").getAsString();
            String msisdn = json.get("msisdn").getAsString();
            double balance = json.get("balance").getAsDouble();
            
            if ("add".equals(action)) {
                // Check if subscriber already exists
                String checkSql = "SELECT COUNT(*) FROM customers WHERE msisdn = ?";
                try (Connection conn = getConnection();
                     PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, msisdn);
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    int count = rs.getInt(1);
                    
                    if (count > 0) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        response.getWriter().write("{\"error\": \"Subscriber already exists\"}");
                        return;
                    }
                }
                
                // Insert new subscriber
                String sql = "INSERT INTO customers (msisdn, balance) VALUES (?, ?)";
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, msisdn);
                    stmt.setDouble(2, balance);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("{\"success\": true, \"message\": \"Subscriber added successfully\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().write("{\"error\": \"Failed to insert subscriber\"}");
                    }
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Invalid action\"}");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorMsg = "{\"error\": \"Invalid request format: " + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        }
    }
    
    // PUT - Update subscriber
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        addCORSHeaders(response);
        
        try {
            // Parse JSON request body
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String action = json.get("action").getAsString();
            String oldMsisdn = json.get("oldMsisdn").getAsString();
            String newMsisdn = json.get("newMsisdn").getAsString();
            double balance = json.get("balance").getAsDouble();
            
            if ("update".equals(action)) {
                // Check if new MSISDN already exists (if changed)
                if (!oldMsisdn.equals(newMsisdn)) {
                    String checkSql = "SELECT COUNT(*) FROM customers WHERE msisdn = ?";
                    try (Connection conn = getConnection();
                         PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setString(1, newMsisdn);
                        ResultSet rs = checkStmt.executeQuery();
                        rs.next();
                        int count = rs.getInt(1);
                        
                        if (count > 0) {
                            response.setStatus(HttpServletResponse.SC_CONFLICT);
                            response.getWriter().write("{\"error\": \"New MSISDN already exists\"}");
                            return;
                        }
                    }
                }
                
                // Update subscriber
                String sql = "UPDATE customers SET msisdn = ?, balance = ? WHERE msisdn = ?";
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, newMsisdn);
                    stmt.setDouble(2, balance);
                    stmt.setString(3, oldMsisdn);
                    int rows = stmt.executeUpdate();
                    
                    if (rows > 0) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("{\"success\": true, \"message\": \"Subscriber updated successfully\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\": \"Subscriber not found\"}");
                    }
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"Invalid action\"}");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorMsg = "{\"error\": \"Invalid request format: " + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        }
    }
    
    // DELETE - Remove subscriber
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        addCORSHeaders(response);
        
        String msisdn = request.getParameter("msisdn");
        
        if (msisdn == null || msisdn.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"MSISDN parameter required\"}");
            return;
        }
        
        try (Connection conn = getConnection()) {
            // First check if subscriber exists
            String checkSql = "SELECT COUNT(*) FROM customers WHERE msisdn = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, msisdn);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);
                
                if (count == 0) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Subscriber not found\"}");
                    return;
                }
            }
            
            // Delete subscriber
            String sql = "DELETE FROM customers WHERE msisdn = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, msisdn);
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"success\": true, \"message\": \"Subscriber deleted successfully\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"error\": \"Failed to delete subscriber\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            response.getWriter().write(errorMsg);
            e.printStackTrace();
        }
    }
}
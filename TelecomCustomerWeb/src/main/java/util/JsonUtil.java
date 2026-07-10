package util;

/**
 * Tiny hand-rolled JSON helpers. The project intentionally avoids pulling in
 * Jackson/Gson so the only third-party dependency remains the PostgreSQL driver.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String successMessage(String message) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"}";
    }

    public static String errorMessage(String message) {
        return "{\"success\":false,\"error\":\"" + escape(message) + "\"}";
    }
}

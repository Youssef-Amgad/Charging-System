package dao;

/**
 * Thrown when a lookup, update, or delete targets an MSISDN that doesn't exist.
 */
public class CustomerNotFoundException extends Exception {
    public CustomerNotFoundException(String msisdn) {
        super("Customer with MSISDN " + msisdn + " was not found.");
    }
}

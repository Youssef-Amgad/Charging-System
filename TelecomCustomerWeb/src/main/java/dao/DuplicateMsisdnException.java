package dao;

/**
 * Thrown when attempting to insert a customer whose MSISDN already exists.
 */
public class DuplicateMsisdnException extends Exception {
    public DuplicateMsisdnException(String msisdn) {
        super("Customer with MSISDN " + msisdn + " already exists.");
    }
}

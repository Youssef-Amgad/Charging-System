package model;

import java.math.BigDecimal;

/**
 * Model class representing a row in the public.customers table.
 * Maps directly to columns: msisdn (varchar PK), balance (numeric).
 */
public class Customer {

    private String msisdn;
    private BigDecimal balance;

    public Customer() {
    }

    public Customer(String msisdn, BigDecimal balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Serializes this customer to a minimal JSON object.
     * Hand-rolled to avoid pulling in an extra JSON library dependency,
     * per the "no extra frameworks" requirement.
     */
    public String toJson() {
        return "{"
                + "\"msisdn\":\"" + escape(msisdn) + "\","
                + "\"balance\":" + (balance != null ? balance.toPlainString() : "0")
                + "}";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

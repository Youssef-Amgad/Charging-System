package com.ocs.model;

import java.math.BigDecimal;

/** Immutable snapshot of a customer row. */
public class Customer {

    private final String     msisdn;
    private final BigDecimal balance;

    public Customer(String msisdn, BigDecimal balance) {
        this.msisdn  = msisdn;
        this.balance = balance;
    }

    public String     getMsisdn()  { return msisdn;  }
    public BigDecimal getBalance() { return balance; }

    @Override
    public String toString() {
        return "Customer{msisdn='" + msisdn + "', balance=" + balance + '}';
    }
}

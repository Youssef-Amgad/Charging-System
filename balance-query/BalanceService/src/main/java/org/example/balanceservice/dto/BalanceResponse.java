package org.example.balanceservice.dto;

public class BalanceResponse {

    private String msisdn;
    private double balance;

    public BalanceResponse() {
    }

    public BalanceResponse(String msisdn, double balance) {
        this.msisdn = msisdn;
        this.balance = balance;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}

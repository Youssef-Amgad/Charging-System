package org.example.balanceservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subscribers")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String msisdn;

    @Column(name = "customer_name")
    private String customerName;

    private Double balance;

    private String status;

    public Long getId() {
        return id;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Double getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
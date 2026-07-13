package org.example.balanceservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    private String msisdn;

    private Double balance;

    public String getMsisdn() {
        return msisdn;
    }
    public Double getBalance() {
        return balance;
    }


}
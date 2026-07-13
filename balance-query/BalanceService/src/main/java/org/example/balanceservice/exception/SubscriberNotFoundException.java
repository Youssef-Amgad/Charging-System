package org.example.balanceservice.exception;

public class SubscriberNotFoundException extends RuntimeException {

    public SubscriberNotFoundException(String msisdn) {
        super("Subscriber " + msisdn + " not found");
    }
}
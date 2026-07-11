package org.example.balanceservice.service;

import org.example.balanceservice.dto.BalanceResponse;
import org.example.balanceservice.entity.Subscriber;
import org.example.balanceservice.exception.SubscriberNotFoundException;
import org.example.balanceservice.repository.SubscriberRepository;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    private final SubscriberRepository repository;

    public BalanceService(SubscriberRepository repository) {
        this.repository = repository;
    }

    public BalanceResponse getBalance(String msisdn) {

        Subscriber subscriber = repository.findByMsisdn(msisdn)
                .orElseThrow(() -> new SubscriberNotFoundException(msisdn));

        return new BalanceResponse(
                subscriber.getMsisdn(),
                subscriber.getBalance()
        );
    }
}
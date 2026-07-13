package org.example.balanceservice.repository;

import org.example.balanceservice.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    Optional<Subscriber> findByMsisdn(String msisdn);

}
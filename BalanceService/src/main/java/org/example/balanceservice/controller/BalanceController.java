package org.example.balanceservice.controller;

import org.example.balanceservice.dto.BalanceResponse;
import org.example.balanceservice.service.BalanceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/{msisdn}")
    public BalanceResponse getBalance(
            @PathVariable String msisdn) {

        return balanceService.getBalance(msisdn);
    }
}

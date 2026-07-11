/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.balancequery.agi;

import com.balancequery.service.BalanceApiClient;
import com.balancequery.model.BalanceResponse;

import java.io.IOException;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

/**
 *
 * @author mohamed
 */


public class BalanceQueryAGI extends BaseAgiScript {

    private final BalanceApiClient apiClient = new BalanceApiClient();

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {

        answer();

        String msisdn = request.getParameter("msisdn");
        System.out.println("MSISDN = " + msisdn);

        try {

            BalanceResponse balanceResponse = apiClient.getBalance(msisdn);

            System.out.println("========== Balance Response ==========");
            System.out.println("MSISDN : " + balanceResponse.getMsisdn());
            System.out.println("Balance: " + balanceResponse.getBalance());
            System.out.println("======================================");

            streamFile("custom/balance-query/balance-is");

            // TODO:
            // هنضيف هنا نطق الرصيد باستخدام أصوات Asterisk

            streamFile("custom/balance-query/goodbye");

        } catch (RuntimeException ex) {

            streamFile("custom/balance-query/invalid-number");

        } catch (IOException | InterruptedException | AgiException ex) {

            streamFile("custom/balance-query/system-error");
        } 

        hangup();
    }
}
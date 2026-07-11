/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.balancequery.agi;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

/**
 *
 * @author mohamed
 */
public class BalanceQueryAGI extends BaseAgiScript {

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        
        System.out.println("========== New Call ==========");
        System.out.println("Caller ID : " + request.getCallerIdNumber());
        System.out.println("Extension : " + request.getExtension());
        System.out.println("MSISDN    : " + request.getParameter("msisdn"));
        System.out.println("==============================");
        
        answer();

        streamFile("custom/balance-query/welcome");

        hangup();
    }
}

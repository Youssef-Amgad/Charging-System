/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.balancequery;
import org.asteriskjava.fastagi.DefaultAgiServer;
/**
 *
 * @author mohamed
 */
public class Main {

    public static void main(String[] args) throws Exception {

        DefaultAgiServer server = new DefaultAgiServer();

        System.out.println("FastAGI Server started on port 4573...");

        server.startup();
    }
}
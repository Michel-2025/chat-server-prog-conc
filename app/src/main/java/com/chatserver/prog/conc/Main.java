/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chatserver.prog.conc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.chatserver.prog.conc",
    "com.chatserver.config",
    "com.chatserver.handler",
    "com.chatserver.metrics"
})
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Main.class, args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("JVM SHUTDOWN HOOK - closing Spring context");
            ctx.close();
            System.out.println("SHUTDOWN completed cleanly");
        }));
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.worker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class WorkerController {

    private static final Logger log = LoggerFactory.getLogger(WorkerController.class);
    private final AtomicLong processed = new AtomicLong();

    @PostMapping("/log")
    public Map<String, Object> logMessage(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        long count = processed.incrementAndGet();
        log.info("WORKER received message #{}: {}", count, message);
        return Map.of(
            "status", "ok",
            "processed", count
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chatserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class WorkerClient {

    private static final Logger log = LoggerFactory.getLogger(WorkerClient.class);
    private static final String WORKER_URL = "http://localhost:8082/log";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public CompletableFuture<Void> sendAsync(String message) {
        String body = "{\"message\": \"" + message.replace("\"", "'") + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    log.info("WORKER response status={}", response.statusCode());
                })
                .exceptionally(ex -> {
                    log.warn("WORKER unreachable, continuing without it: {}", ex.getMessage());
                    return null;
                });
    }
}

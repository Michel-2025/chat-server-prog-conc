/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chatserver.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {

    static final AtomicLong sent = new AtomicLong();
    static final AtomicLong received = new AtomicLong();
    static final AtomicLong errors = new AtomicLong();

    public static void main(String[] args) throws Exception {
        int NUM_CLIENTS = 50;
        int MESSAGES_PER_CLIENT = 20;
        String URL = "ws://localhost:8081/chat/room-loadtest";

        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS);
        CountDownLatch latch = new CountDownLatch(NUM_CLIENTS);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    HttpClient httpClient = HttpClient.newHttpClient();
                    CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                            .connectTimeout(Duration.ofSeconds(5))
                            .buildAsync(URI.create(URL), new WebSocket.Listener() {
                                @Override
                                public void onOpen(WebSocket ws) {
                                    ws.request(Long.MAX_VALUE);
                                }
                                @Override
                                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                                    received.incrementAndGet();
                                    ws.request(1);
                                    return CompletableFuture.completedFuture(null);
                                }
                                @Override
                                public void onError(WebSocket ws, Throwable error) {
                                    errors.incrementAndGet();
                                }
                            });

                    WebSocket ws = wsFuture.get(5, TimeUnit.SECONDS);

                    for (int m = 0; m < MESSAGES_PER_CLIENT; m++) {
                        ws.sendText("Client-" + clientId + " msg-" + m, true).get();
                        sent.incrementAndGet();
                        Thread.sleep(50);
                    }

                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "done").get();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n========= LOAD TEST RESULTS =========");
        System.out.println("Clients        : " + NUM_CLIENTS);
        System.out.println("Messages sent  : " + sent.get());
        System.out.println("Messages recvd : " + received.get());
        System.out.println("Errors         : " + errors.get());
        System.out.println("Duration       : " + duration + " ms");
        System.out.println("Throughput     : " + (sent.get() * 1000 / duration) + " msg/sec");
        System.out.println("=====================================");

        executor.shutdown();
    }
}

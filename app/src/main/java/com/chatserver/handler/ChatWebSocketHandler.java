/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chatserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final int MAX_QUEUE_SIZE = 100;

    
    
    // room -> list of sessions
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> rooms
            = new ConcurrentHashMap<>();

    // session -> bounded message queue
    private final ConcurrentHashMap<String, BlockingQueue<String>> sessionQueues
            = new ConcurrentHashMap<>();

    // metrics
    private final AtomicLong totalReceived = new AtomicLong();
    private final AtomicLong totalDropped = new AtomicLong();
    private final AtomicLong totalSent = new AtomicLong();

    // one thread pool for all send operations
    private final ExecutorService sender = Executors.newFixedThreadPool(4);
    
    private final WorkerClient workerClient = new WorkerClient();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String room = getRoom(session);
        rooms.computeIfAbsent(room, r -> new CopyOnWriteArrayList<>()).add(session);
        sessionQueues.put(session.getId(), new LinkedBlockingQueue<>(MAX_QUEUE_SIZE));
        log.info("CONNECTED session={} room={}", session.getId(), room);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        totalReceived.incrementAndGet();
        workerClient.sendAsync(message.getPayload());
        String room = getRoom(session);
        String payload = "[" + room + "] " + message.getPayload();

        CopyOnWriteArrayList<WebSocketSession> members = rooms.getOrDefault(room, new CopyOnWriteArrayList<>());

        for (WebSocketSession member : members) {
            BlockingQueue<String> queue = sessionQueues.get(member.getId());
            if (queue == null) continue;

            boolean offered = queue.offer(payload);
            if (!offered) {
                totalDropped.incrementAndGet();
                log.warn("DROPPED message for slow client session={}", member.getId());
                continue;
            }

            sender.submit(() -> {
    try {
        String msg = queue.poll();
        if (msg != null && member.isOpen()) {
            synchronized (member) {
                member.sendMessage(new TextMessage(msg));
            }
            totalSent.incrementAndGet();
        }
    } catch (Exception e) {
        log.error("SEND FAILED session={} error={}", member.getId(), e.getMessage());
    }
});
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String room = getRoom(session);
        CopyOnWriteArrayList<WebSocketSession> members = rooms.get(room);
        if (members != null) members.remove(session);
        sessionQueues.remove(session.getId());
        log.info("DISCONNECTED session={} room={} status={}", session.getId(), room, status);
    }

    private String getRoom(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts.length >= 3 ? parts[2] : "default";
    }

    public long getReceived() { return totalReceived.get(); }
    public long getDropped()  { return totalDropped.get(); }
    public long getSent()     { return totalSent.get(); }

@jakarta.annotation.PreDestroy
public void shutdown() {
    log.info("SHUTDOWN initiated - stopping sender thread pool");
    sender.shutdown();
    try {
        if (!sender.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
            sender.shutdownNow();
            log.warn("SHUTDOWN forced after timeout");
        } else {
            log.info("SHUTDOWN completed cleanly");
        }
    } catch (InterruptedException e) {
        sender.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
}

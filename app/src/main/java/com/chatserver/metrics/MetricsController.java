/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chatserver.metrics;

import com.chatserver.handler.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class MetricsController {

    private final ChatWebSocketHandler handler;

    public MetricsController(ChatWebSocketHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/metrics")
    public Map<String, Long> metrics() {
        return Map.of(
            "messages_received", handler.getReceived(),
            "messages_sent",     handler.getSent(),
            "messages_dropped",  handler.getDropped()
        );
    }
}
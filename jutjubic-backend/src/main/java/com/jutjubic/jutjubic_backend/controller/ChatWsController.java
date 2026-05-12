package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send") // klijent šalje na /app/chat.send
    public void send(ChatMessageDto incoming, Principal principal) {

        if (principal == null || principal.getName() == null) return;
        if (incoming == null || incoming.getVideoId() == null) return;

        String msg = incoming.getMessage() == null ? "" : incoming.getMessage().trim();
        if (msg.isEmpty()) return;
        if (msg.length() > 500) msg = msg.substring(0, 500);

        ChatMessageDto outgoing = new ChatMessageDto();
        outgoing.setVideoId(incoming.getVideoId());
        outgoing.setMessage(msg);
        outgoing.setSender(principal.getName()); // email
        outgoing.setTimestamp(Instant.now());

        messagingTemplate.convertAndSend("/topic/chat." + incoming.getVideoId(), outgoing);
    }
}

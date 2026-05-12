package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.PlayVideoMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WatchPartyWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Kada vlasnik pusti video
    @MessageMapping("/watchparty/{roomId}/play")
    public void playVideo(@DestinationVariable String roomId, @Payload PlayVideoMessage msg) {

        // Proveri da li je pošiljalac vlasnik sobe

        // Pošalji svim članovima sobe da otvore video
        messagingTemplate.convertAndSend(
                "/topic/watchparty/" + roomId,
                msg  // { videoId: 123 }
        );
    }
}

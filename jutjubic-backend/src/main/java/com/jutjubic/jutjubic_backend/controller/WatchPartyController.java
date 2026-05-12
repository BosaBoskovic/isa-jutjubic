package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.model.WatchParty;
import org.springframework.web.bind.annotation.*;
import com.jutjubic.jutjubic_backend.dto.CreateRoomRequest;
import com.jutjubic.jutjubic_backend.dto.WatchPartyDetails;
import com.jutjubic.jutjubic_backend.model.WatchParty;
import com.jutjubic.jutjubic_backend.service.WatchPartyService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/watchparty")
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    public WatchPartyController(WatchPartyService watchPartyService) {
        this.watchPartyService = watchPartyService;
    }

    // Kreiranje sobe
    @PostMapping
    public WatchParty createRoom(Authentication auth, @RequestBody CreateRoomRequest req) {
        String email = auth.getName();
        return watchPartyService.createRoom(email, req.getName());
    }

    // Lista svih aktivnih soba
    @GetMapping
    public List<WatchParty> getActiveRooms() {
        return watchPartyService.getActiveRooms();
    }

    // Detalji sobe
    @GetMapping("/{inviteCode}")
    public WatchPartyDetails getRoomDetails(@PathVariable String inviteCode) {
        return watchPartyService.getRoomDetails(inviteCode);
    }

    @DeleteMapping("/{inviteCode}")
    public void closeRoom(@PathVariable String inviteCode, Authentication auth) {
        watchPartyService.closeRoom(inviteCode, auth.getName());
    }

}

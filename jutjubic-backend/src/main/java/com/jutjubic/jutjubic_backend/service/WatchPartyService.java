package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.WatchPartyDetails;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.WatchParty;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.WatchPartyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;
    private final UserRepository userRepository;

    public WatchPartyService(WatchPartyRepository watchPartyRepository, UserRepository userRepository) {
        this.watchPartyRepository = watchPartyRepository;
        this.userRepository = userRepository;
    }

    public WatchParty createRoom(String email, String name) {
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WatchParty room = new WatchParty();
        room.setName(name);
        room.setCreator(creator);
        room.setInviteCode(generateInviteCode());
        room.setCreatedAt(LocalDateTime.now());
        room.setActive(true);

        return watchPartyRepository.save(room);
    }

    public List<WatchParty> getActiveRooms() {
        return watchPartyRepository.findByActiveTrue();
    }

    public WatchPartyDetails getRoomDetails(String inviteCode) {
        WatchParty room = watchPartyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        WatchPartyDetails details = new WatchPartyDetails();
        details.setId(room.getId());
        details.setName(room.getName());
        details.setInviteCode(room.getInviteCode());
        details.setCreatorUsername(room.getCreator().getUsername());
        details.setCreatedAt(room.getCreatedAt());
        details.setActive(room.isActive());
        details.setMemberUsernames(new ArrayList<>());  // TODO: dodaj članove ako čuvaš u bazi

        return details;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void closeRoom(String inviteCode, String email) {
        WatchParty room = watchPartyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Proveri da li je vlasnik
        if (!room.getCreator().getEmail().equals(email)) {
            throw new RuntimeException("Only creator can close the room");
        }

        room.setActive(false);
        watchPartyRepository.save(room);
    }
}
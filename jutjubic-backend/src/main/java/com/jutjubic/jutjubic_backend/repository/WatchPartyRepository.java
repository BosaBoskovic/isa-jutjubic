package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {

    List<WatchParty> findByActiveTrue();

    Optional<WatchParty> findByInviteCode(String inviteCode);
}

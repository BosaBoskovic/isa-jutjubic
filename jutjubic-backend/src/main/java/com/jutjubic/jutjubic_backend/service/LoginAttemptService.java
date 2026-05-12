package com.jutjubic.jutjubic_backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip){
        Attempt attempt = attemptsCache.get(ip);
        if(attempt == null) return false;
        if(attempt.time.plusMinutes(1).isBefore(LocalDateTime.now())){
            attemptsCache.remove(ip);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }

    public void loginSucceeded(String ip){
        attemptsCache.remove(ip);
    }

    public void loginFailed(String ip){
        Attempt attempt = attemptsCache.get(ip);
        if(attempt == null){
            attempt = new Attempt(1, LocalDateTime.now());
        }else{
            attempt.count++;
        }
        attemptsCache.put(ip, attempt);
    }

    private static class Attempt{
        int count;
        LocalDateTime time;

        Attempt(int count, LocalDateTime time){
            this.count = count;
            this.time = time;
        }
    }
}

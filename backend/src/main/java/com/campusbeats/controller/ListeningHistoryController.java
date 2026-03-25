package com.campusbeats.controller;

import com.campusbeats.dto.ListeningHistoryDTO;
import com.campusbeats.dto.TrackDTO;
import com.campusbeats.service.ListeningHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listening-history")

public class ListeningHistoryController {
    
    @Autowired
    private ListeningHistoryService listeningHistoryService;
    
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<ListeningHistoryDTO>> getRecentlyPlayed(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ListeningHistoryDTO> recentlyPlayed = listeningHistoryService.getRecentlyPlayedByUser(userId, limit);
        return ResponseEntity.ok(recentlyPlayed);
    }
    
    @GetMapping("/user/{userId}/recent-tracks")
    public ResponseEntity<List<TrackDTO>> getRecentlyPlayedTracksOnly(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TrackDTO> recentlyPlayedTracks = listeningHistoryService.getRecentlyPlayedTracksOnly(userId, limit);
        return ResponseEntity.ok(recentlyPlayedTracks);
    }
    
    @PostMapping("/record")
    public ResponseEntity<ListeningHistoryDTO> recordListeningHistory(
            @RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String trackId = (String) request.get("trackId");
            Integer playDuration = request.get("playDuration") != null ? 
                Integer.valueOf(request.get("playDuration").toString()) : null;
            Double completionPercentage = request.get("completionPercentage") != null ? 
                Double.valueOf(request.get("completionPercentage").toString()) : null;
            
            ListeningHistoryDTO history = listeningHistoryService.recordListeningHistory(
                userId, trackId, playDuration, completionPercentage);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/record-simple")
    public ResponseEntity<ListeningHistoryDTO> recordSimpleListeningHistory(
            @RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String trackId = request.get("trackId");
            
            ListeningHistoryDTO history = listeningHistoryService.recordSimpleListeningHistory(userId, trackId);
            return ResponseEntity.status(HttpStatus.CREATED).body(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/user/{userId}/total-time")
    public ResponseEntity<Map<String, Long>> getTotalListeningTime(@PathVariable String userId) {
        Long totalTime = listeningHistoryService.getTotalListeningTime(userId);
        return ResponseEntity.ok(Map.of("totalListeningTime", totalTime));
    }
    
    @GetMapping("/user/{userId}/has-played/{trackId}")
    public ResponseEntity<Map<String, Boolean>> hasUserPlayedTrack(
            @PathVariable String userId, 
            @PathVariable String trackId) {
        boolean hasPlayed = listeningHistoryService.hasUserPlayedTrack(userId, trackId);
        return ResponseEntity.ok(Map.of("hasPlayed", hasPlayed));
    }
    
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<ListeningHistoryDTO>> getListeningHistoryByDateRange(
            @PathVariable String userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            List<ListeningHistoryDTO> history = listeningHistoryService.getListeningHistoryByDateRange(userId, start, end);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

package com.campusbeats.controller;

import com.campusbeats.dto.QueueItemDTO;
import com.campusbeats.service.QueueVotingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/queue")

public class QueueVotingController {
    
    @Autowired
    private QueueVotingService queueVotingService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @PostMapping("/{queueItemId}/vote")
    public ResponseEntity<QueueItemDTO> voteForTrack(
            @PathVariable String queueItemId,
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String roomId = request.get("roomId");
        
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<QueueItemDTO> queueItemOpt = queueVotingService.voteForTrack(queueItemId, userId);
        
        if (queueItemOpt.isPresent()) {
            QueueItemDTO queueItem = queueItemOpt.get();
            
            // Broadcast vote update via WebSocket if roomId is provided
            if (roomId != null) {
                Map<String, Object> voteUpdate = Map.of(
                    "type", "VOTE_ADDED",
                    "queueItemId", queueItemId,
                    "voteCount", queueItem.getVoteCount(),
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/votes", voteUpdate);
            }
            
            return ResponseEntity.ok(queueItem);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @DeleteMapping("/{queueItemId}/vote")
    public ResponseEntity<QueueItemDTO> removeVoteForTrack(
            @PathVariable String queueItemId,
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String roomId = request.get("roomId");
        
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<QueueItemDTO> queueItemOpt = queueVotingService.removeVoteForTrack(queueItemId, userId);
        
        if (queueItemOpt.isPresent()) {
            QueueItemDTO queueItem = queueItemOpt.get();
            
            // Broadcast vote update via WebSocket if roomId is provided
            if (roomId != null) {
                Map<String, Object> voteUpdate = Map.of(
                    "type", "VOTE_REMOVED",
                    "queueItemId", queueItemId,
                    "voteCount", queueItem.getVoteCount(),
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                );
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/votes", voteUpdate);
            }
            
            return ResponseEntity.ok(queueItem);
        }
        
        return ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/{queueItemId}/vote-status")
    public ResponseEntity<Map<String, Object>> getVoteStatus(
            @PathVariable String queueItemId,
            @RequestParam String userId) {
        boolean hasVoted = queueVotingService.hasUserVoted(queueItemId, userId);
        int voteCount = queueVotingService.getVoteCount(queueItemId);
        
        Map<String, Object> status = Map.of(
            "hasVoted", hasVoted,
            "voteCount", voteCount
        );
        
        return ResponseEntity.ok(status);
    }
}

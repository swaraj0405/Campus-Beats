package com.campusbeats.controller;

import com.campusbeats.dto.ListeningRoomDTO;
import com.campusbeats.dto.QueueItemDTO;
import com.campusbeats.entity.ListeningRoom;
import com.campusbeats.service.ListeningRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")

public class ListeningRoomController {
    
    @Autowired
    private ListeningRoomService listeningRoomService;
    
    @GetMapping
    public ResponseEntity<List<ListeningRoomDTO>> getAllActiveRooms() {
        List<ListeningRoomDTO> rooms = listeningRoomService.getAllActiveRooms();
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ListeningRoomDTO> getRoomById(@PathVariable String id) {
        return listeningRoomService.getRoomById(id)
                .map(room -> ResponseEntity.ok(room))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/host/{hostId}")
    public ResponseEntity<List<ListeningRoomDTO>> getRoomsByHost(@PathVariable String hostId) {
        List<ListeningRoomDTO> rooms = listeningRoomService.getRoomsByHost(hostId);
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ListeningRoomDTO>> searchActiveRooms(@RequestParam String query) {
        List<ListeningRoomDTO> rooms = listeningRoomService.searchActiveRooms(query);
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/participant/{userId}")
    public ResponseEntity<List<ListeningRoomDTO>> getRoomsByParticipant(@PathVariable String userId) {
        List<ListeningRoomDTO> rooms = listeningRoomService.getRoomsByParticipant(userId);
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/university/{domain}")
    public ResponseEntity<List<ListeningRoomDTO>> getRoomsByUniversityDomain(@PathVariable String domain) {
        List<ListeningRoomDTO> rooms = listeningRoomService.getRoomsByUniversityDomain(domain);
        return ResponseEntity.ok(rooms);
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<ListeningRoomDTO>> getMostPopularRooms(@RequestParam(defaultValue = "10") int limit) {
        List<ListeningRoomDTO> rooms = listeningRoomService.getMostPopularRooms(limit);
        return ResponseEntity.ok(rooms);
    }
    
    @PostMapping
    public ResponseEntity<ListeningRoomDTO> createRoom(@Valid @RequestBody ListeningRoom room) {
        try {
            ListeningRoomDTO createdRoom = listeningRoomService.createRoom(room);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ListeningRoomDTO> updateRoom(
            @PathVariable String id, 
            @Valid @RequestBody ListeningRoom roomDetails) {
        return listeningRoomService.updateRoom(id, roomDetails)
                .map(room -> ResponseEntity.ok(room))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String id) {
        if (listeningRoomService.deleteRoom(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ListeningRoomDTO> joinRoom(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return listeningRoomService.joinRoom(roomId, userId)
                .map(room -> ResponseEntity.ok(room))
                .orElse(ResponseEntity.badRequest().build());
    }
    
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ListeningRoomDTO> leaveRoom(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return listeningRoomService.leaveRoom(roomId, userId)
                .map(room -> ResponseEntity.ok(room))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{roomId}/playback")
    public ResponseEntity<ListeningRoomDTO> updatePlaybackState(
            @PathVariable String roomId,
            @RequestBody Map<String, Object> request) {
        String trackId = (String) request.get("trackId");
        Long position = request.get("position") != null ? 
                Long.valueOf(request.get("position").toString()) : null;
        Boolean isPlaying = (Boolean) request.get("isPlaying");
        
        return listeningRoomService.updatePlaybackState(roomId, trackId, position, isPlaying)
                .map(room -> ResponseEntity.ok(room))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{roomId}/queue")
    public ResponseEntity<QueueItemDTO> addTrackToQueue(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {
        String trackId = request.get("trackId");
        String userId = request.get("userId");
        
        if (trackId == null || userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return listeningRoomService.addTrackToQueue(roomId, trackId, userId)
                .map(queueItem -> ResponseEntity.status(HttpStatus.CREATED).body(queueItem))
                .orElse(ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/{roomId}/queue")
    public ResponseEntity<List<QueueItemDTO>> getRoomQueue(
            @PathVariable String roomId,
            @RequestParam(required = false) String userId) {
        List<QueueItemDTO> queue = listeningRoomService.getRoomQueue(roomId, userId);
        return ResponseEntity.ok(queue);
    }
}

package com.campusbeats.controller;

import com.campusbeats.dto.ListeningRoomDTO;
import com.campusbeats.dto.QueueItemDTO;
import com.campusbeats.service.ListeningRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

@Controller
public class WebSocketController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ListeningRoomService listeningRoomService;
    
    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        Optional<ListeningRoomDTO> roomOpt = listeningRoomService.joinRoom(roomId, userId);
        
        if (roomOpt.isPresent()) {
            ListeningRoomDTO room = roomOpt.get();
            
            // Notify all participants in the room about the new user
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/participants", room.getParticipants());
            
            // Send join notification
            Map<String, Object> joinMessage = Map.of(
                "type", "USER_JOINED",
                "userId", userId,
                "userName", userName,
                "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", joinMessage);
        }
    }
    
    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        Optional<ListeningRoomDTO> roomOpt = listeningRoomService.leaveRoom(roomId, userId);
        
        if (roomOpt.isPresent()) {
            ListeningRoomDTO room = roomOpt.get();
            
            // Notify all participants in the room about the user leaving
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/participants", room.getParticipants());
            
            // Send leave notification
            Map<String, Object> leaveMessage = Map.of(
                "type", "USER_LEFT",
                "userId", userId,
                "userName", userName,
                "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", leaveMessage);
        }
    }
    
    @MessageMapping("/room/{roomId}/playback")
    public void updatePlayback(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        String trackId = (String) payload.get("trackId");
        Long position = payload.get("position") != null ? 
                Long.valueOf(payload.get("position").toString()) : null;
        Boolean isPlaying = (Boolean) payload.get("isPlaying");
        String userId = (String) payload.get("userId");
        
        Optional<ListeningRoomDTO> roomOpt = listeningRoomService.updatePlaybackState(roomId, trackId, position, isPlaying);
        
        if (roomOpt.isPresent()) {
            ListeningRoomDTO room = roomOpt.get();
            
            // Broadcast playback state to all participants
            Map<String, Object> playbackState = Map.of(
                "type", "PLAYBACK_UPDATE",
                "currentTrack", room.getCurrentTrack() != null ? room.getCurrentTrack() : Map.of(),
                "currentPosition", room.getCurrentPosition(),
                "isPlaying", room.getIsPlaying(),
                "updatedBy", userId,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/playback", playbackState);
        }
    }
    
    @MessageMapping("/room/{roomId}/queue/add")
    public void addToQueue(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String trackId = payload.get("trackId");
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        
        Optional<QueueItemDTO> queueItemOpt = listeningRoomService.addTrackToQueue(roomId, trackId, userId);
        
        if (queueItemOpt.isPresent()) {
            QueueItemDTO queueItem = queueItemOpt.get();
            
            // Broadcast queue update to all participants
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/queue", 
                    listeningRoomService.getRoomQueue(roomId, null));
            
            // Send queue addition notification
            Map<String, Object> queueMessage = Map.of(
                "type", "TRACK_ADDED_TO_QUEUE",
                "track", queueItem.getTrack(),
                "addedBy", userName,
                "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", queueMessage);
        }
    }
    
    @MessageMapping("/room/{roomId}/chat")
    public void sendChatMessage(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String userName = payload.get("userName");
        String message = payload.get("message");
        
        // Create chat message object
        Map<String, Object> chatMessage = Map.of(
            "type", "CHAT_MESSAGE",
            "userId", userId,
            "userName", userName,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        // Broadcast chat message to all participants in the room
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", chatMessage);
    }
    
    @MessageMapping("/room/{roomId}/sync")
    public void syncPlayback(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        
        // Get current room state and send to requesting user
        Optional<ListeningRoomDTO> roomOpt = listeningRoomService.getRoomById(roomId);
        
        if (roomOpt.isPresent()) {
            ListeningRoomDTO room = roomOpt.get();
            
            Map<String, Object> syncData = Map.of(
                "type", "SYNC_RESPONSE",
                "currentTrack", room.getCurrentTrack() != null ? room.getCurrentTrack() : Map.of(),
                "currentPosition", room.getCurrentPosition(),
                "isPlaying", room.getIsPlaying(),
                "timestamp", System.currentTimeMillis()
            );
            
            // Send sync data to specific user
            messagingTemplate.convertAndSendToUser(userId, "/queue/room/" + roomId + "/sync", syncData);
        }
    }
}

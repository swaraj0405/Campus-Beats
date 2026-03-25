package com.campusbeats.controller;

import com.campusbeats.dto.TrackDTO;
import com.campusbeats.entity.Track;
import com.campusbeats.entity.User;
import com.campusbeats.service.TrackService;
import com.campusbeats.service.UserService;
import com.campusbeats.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")

public class FileUploadController {
    
    @Autowired
    private TrackService trackService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/track")
    public ResponseEntity<?> uploadTrack(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("genre") String genre,
            @RequestParam("album") String album,
            @RequestParam("uploaderId") String uploaderId) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Please select a file to upload"));
            }
            
            // Validate file type (audio files only)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Only audio files are allowed"));
            }
            
            // Validate file size (max 50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("File size must be less than 50MB"));
            }
            
            // Get uploader
            User uploader = userService.getUserEntityById(uploaderId);
            if (uploader == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid uploader ID"));
            }
            
            // Store file and get URL
            String fileUrl = fileStorageService.storeAudioFile(file, uploaderId);
            
            // Get audio duration (mock for now - in real implementation, use audio processing library)
            int duration = 180; // Default 3 minutes
            
            // Create track entity
            Track track = new Track();
            track.setTitle(title);
            track.setArtist(uploader.getName());
            track.setAlbum(album.isEmpty() ? null : album);
            track.setGenre(genre);
            track.setUrl(fileUrl);
            track.setDuration(duration);
            track.setUploader(uploader);
            track.setCoverArt(generateCoverArtUrl(title));
            
            // Save track
            TrackDTO savedTrack = trackService.createTrack(track);
            
            // Award points to user
            userService.addPointsToUser(uploaderId, 10);
            
            return ResponseEntity.ok(savedTrack);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to upload track: " + e.getMessage()));
        }
    }
    
    @PostMapping("/cover-art")
    public ResponseEntity<?> uploadCoverArt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("trackId") String trackId) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Please select a file to upload"));
            }
            
            // Validate file type (image files only)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Only image files are allowed"));
            }
            
            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("File size must be less than 5MB"));
            }
            
            // Store file and get URL
            String imageUrl = fileStorageService.storeImageFile(file, trackId);
            
            // Update track with cover art URL
            Track track = trackService.getTrackEntityById(trackId);
            if (track == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Track not found"));
            }
            
            track.setCoverArt(imageUrl);
            TrackDTO updatedTrack = trackService.updateTrack(trackId, track);
            
            return ResponseEntity.ok(updatedTrack);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to upload cover art: " + e.getMessage()));
        }
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
    
    private String generateCoverArtUrl(String title) {
        // Generate a placeholder cover art URL based on title
        return "https://picsum.photos/seed/" + title.hashCode() + "/300/300";
    }
}

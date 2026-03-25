package com.campusbeats.controller;

import com.campusbeats.dto.TrackDTO;
import com.campusbeats.entity.Track;
import com.campusbeats.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tracks")

public class TrackController {
    
    @Autowired
    private TrackService trackService;
    
    @GetMapping
    public ResponseEntity<List<TrackDTO>> getAllTracks() {
        List<TrackDTO> tracks = trackService.getAllTracks();
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TrackDTO> getTrackById(@PathVariable String id) {
        return trackService.getTrackById(id)
                .map(track -> ResponseEntity.ok(track))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<TrackDTO>> getTracksByGenre(@PathVariable String genre) {
        List<TrackDTO> tracks = trackService.getActiveTracksByGenre(genre);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<TrackDTO>> getTracksByUploader(@PathVariable String uploaderId) {
        List<TrackDTO> tracks = trackService.getTracksByUploader(uploaderId);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<TrackDTO>> getPopularTracks(@RequestParam(defaultValue = "20") int limit) {
        List<TrackDTO> tracks = trackService.getPopularTracks(limit);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<TrackDTO>> getRecentTracks(@RequestParam(defaultValue = "20") int limit) {
        List<TrackDTO> tracks = trackService.getRecentTracks(limit);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<TrackDTO>> searchTracks(@RequestParam String query) {
        List<TrackDTO> tracks = trackService.searchTracks(query);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/university/{domain}")
    public ResponseEntity<List<TrackDTO>> getTracksByUniversityDomain(@PathVariable String domain) {
        List<TrackDTO> tracks = trackService.getTracksByUniversityDomain(domain);
        return ResponseEntity.ok(tracks);
    }
    
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        List<String> genres = trackService.getAllGenres();
        return ResponseEntity.ok(genres);
    }
    
    @GetMapping("/top/{genre}")
    public ResponseEntity<List<TrackDTO>> getTopTracksByGenre(
            @PathVariable String genre, 
            @RequestParam(defaultValue = "10") int limit) {
        List<TrackDTO> tracks = trackService.getTopTracksByGenre(genre, limit);
        return ResponseEntity.ok(tracks);
    }
    
    @PostMapping
    public ResponseEntity<TrackDTO> createTrack(@Valid @RequestBody Track track) {
        try {
            TrackDTO createdTrack = trackService.createTrack(track);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTrack);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TrackDTO> updateTrack(@PathVariable String id, @Valid @RequestBody Track trackDetails) {
        TrackDTO updatedTrack = trackService.updateTrack(id, trackDetails);
        if (updatedTrack != null) {
            return ResponseEntity.ok(updatedTrack);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrack(@PathVariable String id) {
        if (trackService.deleteTrack(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{id}/play")
    public ResponseEntity<TrackDTO> incrementPlayCount(@PathVariable String id) {
        return trackService.incrementPlayCount(id)
                .map(track -> ResponseEntity.ok(track))
                .orElse(ResponseEntity.notFound().build());
    }
}

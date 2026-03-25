package com.campusbeats.controller;

import com.campusbeats.dto.PlaylistDTO;
import com.campusbeats.entity.Playlist;
import com.campusbeats.entity.PlaylistCategory;
import com.campusbeats.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")

public class PlaylistController {
    
    @Autowired
    private PlaylistService playlistService;
    
    @GetMapping
    public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getAllPlaylists();
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDTO> getPlaylistById(@PathVariable String id) {
        return playlistService.getPlaylistById(id)
                .map(playlist -> ResponseEntity.ok(playlist))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/public")
    public ResponseEntity<List<PlaylistDTO>> getPublicPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getPublicPlaylists();
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByCategory(@PathVariable String category) {
        try {
            PlaylistCategory playlistCategory = PlaylistCategory.valueOf(category.toUpperCase());
            List<PlaylistDTO> playlists = playlistService.getPlaylistsByCategory(playlistCategory);
            return ResponseEntity.ok(playlists);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByCreator(@PathVariable String creatorId) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsByCreator(creatorId);
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<PlaylistDTO>> searchPlaylists(@RequestParam String query) {
        List<PlaylistDTO> playlists = playlistService.searchPlaylists(query);
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/university/{domain}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByUniversityDomain(@PathVariable String domain) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsByUniversityDomain(domain);
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/containing-track/{trackId}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsContainingTrack(@PathVariable String trackId) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsContainingTrack(trackId);
        return ResponseEntity.ok(playlists);
    }
    
    @GetMapping("/by-track-count")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByTrackCount(
            @RequestParam(defaultValue = "1") int minTracks,
            @RequestParam(defaultValue = "100") int maxTracks) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsByTrackCount(minTracks, maxTracks);
        return ResponseEntity.ok(playlists);
    }
    
    @PostMapping
    public ResponseEntity<PlaylistDTO> createPlaylist(@Valid @RequestBody Playlist playlist) {
        try {
            PlaylistDTO createdPlaylist = playlistService.createPlaylist(playlist);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPlaylist);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PlaylistDTO> updatePlaylist(
            @PathVariable String id, 
            @Valid @RequestBody Playlist playlistDetails) {
        return playlistService.updatePlaylist(id, playlistDetails)
                .map(playlist -> ResponseEntity.ok(playlist))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable String id) {
        if (playlistService.deletePlaylist(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistDTO> addTrackToPlaylist(
            @PathVariable String playlistId,
            @RequestBody Map<String, String> request) {
        String trackId = request.get("trackId");
        if (trackId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return playlistService.addTrackToPlaylist(playlistId, trackId)
                .map(playlist -> ResponseEntity.ok(playlist))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<PlaylistDTO> removeTrackFromPlaylist(
            @PathVariable String playlistId,
            @PathVariable String trackId) {
        return playlistService.removeTrackFromPlaylist(playlistId, trackId)
                .map(playlist -> ResponseEntity.ok(playlist))
                .orElse(ResponseEntity.notFound().build());
    }
}

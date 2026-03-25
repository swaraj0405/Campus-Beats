package com.campusbeats.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")

public class FileController {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @GetMapping("/audio/{filename:.+}")
    public ResponseEntity<Resource> serveAudioFile(@PathVariable String filename) {
        return serveFile("audio", filename);
    }
    
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImageFile(@PathVariable String filename) {
        return serveFile("images", filename);
    }
    
    private ResponseEntity<Resource> serveFile(String subDir, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, subDir, filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = determineContentType(filename, subDir);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private String determineContentType(String filename, String subDir) {
        String extension = "";
        if (filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        
        if ("audio".equals(subDir)) {
            switch (extension) {
                case "mp3":
                    return "audio/mpeg";
                case "wav":
                    return "audio/wav";
                case "ogg":
                    return "audio/ogg";
                case "m4a":
                    return "audio/mp4";
                case "flac":
                    return "audio/flac";
                default:
                    return "audio/mpeg"; // Default to MP3
            }
        } else if ("images".equals(subDir)) {
            switch (extension) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                default:
                    return "image/jpeg"; // Default to JPEG
            }
        }
        
        return "application/octet-stream";
    }
}

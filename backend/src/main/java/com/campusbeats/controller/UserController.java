package com.campusbeats.controller;

import com.campusbeats.dto.UserDTO;
import com.campusbeats.entity.User;
import com.campusbeats.entity.BadgeType;
import com.campusbeats.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/university/{domain}")
    public ResponseEntity<List<UserDTO>> getUsersByUniversityDomain(@PathVariable String domain) {
        List<UserDTO> users = userService.getUsersByUniversityDomain(domain);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/top")
    public ResponseEntity<List<UserDTO>> getTopUsersByPoints(@RequestParam(defaultValue = "10") int limit) {
        List<UserDTO> users = userService.getTopUsersByPoints(limit);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody User user) {
        try {
            UserDTO createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable String id, @Valid @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{id}/points")
    public ResponseEntity<UserDTO> addPointsToUser(
            @PathVariable String id, 
            @RequestBody Map<String, Integer> request) {
        Integer points = request.get("points");
        if (points == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return userService.addPointsToUser(id, points)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/badges")
    public ResponseEntity<UserDTO> addBadgeToUser(
            @PathVariable String id, 
            @RequestBody Map<String, String> request) {
        String badgeName = request.get("badge");
        if (badgeName == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            BadgeType badge = BadgeType.valueOf(badgeName.toUpperCase());
            return userService.addBadgeToUser(id, badge)
                    .map(user -> ResponseEntity.ok(user))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

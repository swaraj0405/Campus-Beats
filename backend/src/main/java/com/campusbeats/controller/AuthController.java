package com.campusbeats.controller;

import com.campusbeats.dto.LoginRequest;
import com.campusbeats.dto.RegisterRequest;
import com.campusbeats.dto.AuthResponse;
import com.campusbeats.dto.UserDTO;
import com.campusbeats.entity.User;
import com.campusbeats.service.AuthService;
import com.campusbeats.service.EmailService;
import com.campusbeats.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userService.getUserByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "User with this email already exists"));
            }
            
            // Create new user
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword()); // Will be hashed in service
            user.setUniversityDomain(request.getEmail().split("@")[1]);
            
            userService.createUser(user);
            
            // Send verification email
            User userEntity = userService.getUserEntityByEmail(request.getEmail());
            emailService.sendVerificationEmail(userEntity.getEmail(), userEntity.getVerificationToken());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful! Please check your email to verify your account."));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Check if user exists first
            Optional<UserDTO> userOpt = userService.getUserByEmail(request.getEmail());
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No account found with this email address"));
            }
            
            // Authenticate user
            boolean isAuthenticated = authService.authenticate(request.getEmail(), request.getPassword());
            
            if (!isAuthenticated) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Incorrect password. Please try again"));
            }
            
            // Check if email is verified
            User userEntity = userService.getUserEntityByEmail(request.getEmail());
            if (userEntity == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            if (!userEntity.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "error", "Email not verified",
                        "message", "Please verify your email before logging in. Check your inbox for the verification link.",
                        "emailVerified", false
                    ));
            }
            
            // Get user details
            UserDTO user = userService.getUserByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Generate JWT token
            String token = authService.generateToken(request.getEmail());
            
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(user);
            response.setMessage("Login successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authorization header"));
            }
            
            String token = authHeader.substring(7);
            String email = authService.validateToken(token);
            
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
            }
            
            UserDTO user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "user", user
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Token validation failed: " + e.getMessage()));
        }
    }
    

    
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            boolean isVerified = userService.verifyEmail(token);
            
            if (isVerified) {
                return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully! You can now log in to your account.",
                    "verified", true
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired verification token"));
            }
            
        } catch (Exception e) {
             return ResponseEntity.badRequest()
                 .body(Map.of("error", "Email verification failed: " + e.getMessage()));
         }
      }
    
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            boolean codeSent = userService.sendVerificationCode(email);
            
            if (codeSent) {
                return ResponseEntity.ok(Map.of(
                    "message", "Verification code sent to your email",
                    "success", true
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to send verification code"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to send verification code: " + e.getMessage()));
        }
    }
    
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        try {
            boolean isValid = userService.verifyCode(email, code);
            
            if (isValid) {
                return ResponseEntity.ok(Map.of(
                    "message", "Verification code is valid",
                    "verified", true
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired verification code"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Code verification failed: " + e.getMessage()));
        }
     }
     
     @PostMapping("/complete-registration")
     public ResponseEntity<?> completeRegistration(@RequestBody Map<String, String> request) {
         String email = request.get("email");
         String name = request.get("name");
         String password = request.get("password");
         try {
             
             // Get the temporary user and complete registration
              User user = userService.getUserEntityByEmail(email);
              if (user == null) {
                  return ResponseEntity.badRequest()
                      .body(Map.of("error", "User not found for verification"));
              }
              
              // Check if user already has complete registration
              if (user.getIsEmailVerified() && !user.getName().equals("TEMP_USER")) {
                  return ResponseEntity.badRequest()
                      .body(Map.of("error", "User with this email already exists"));
              }
              
              // Complete user registration
              user.setName(name);
              user.setPassword(password); // Will be hashed in service
              user.setIsEmailVerified(true);
              
              UserDTO updatedUser = userService.updateUserForRegistration(user);
             
             // Generate JWT token for auto-login
             String token = authService.generateToken(email);
             
             return ResponseEntity.status(HttpStatus.CREATED)
                  .body(Map.of(
                      "message", "Registration completed successfully!",
                      "token", token,
                      "user", updatedUser
                  ));
             
         } catch (Exception e) {
             return ResponseEntity.badRequest()
                 .body(Map.of("error", "Registration failed: " + e.getMessage()));
         }
     }

     
     @PostMapping("/logout")
     public ResponseEntity<?> logout() {
         // For JWT, logout is handled client-side by removing the token
         return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
     }
     


}

package com.campusbeats.service;

import com.campusbeats.dto.UserDTO;
import com.campusbeats.entity.User;
import com.campusbeats.entity.BadgeType;
import com.campusbeats.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private EmailService emailService;
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public Optional<UserDTO> getUserById(String id) {
        Long idLong = Long.parseLong(id);
        return userRepository.findById(idLong)
                .map(this::convertToDTO);
    }
    
    public User getUserEntityById(String id) {
        Long idLong = Long.parseLong(id);
        return userRepository.findById(idLong).orElse(null);
    }
    
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }
    
    public List<UserDTO> getUsersByUniversityDomain(String domain) {
        return userRepository.findTopUsersByPointsAndDomain(domain).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> getTopUsersByPoints(int limit) {
        return userRepository.findTopUsersByPoints().stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> searchUsers(String query) {
        return userRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public UserDTO createUser(User user) {
        // Hash the password before saving
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(authService.hashPassword(user.getPassword()));
        }
        
        // Set email verification status
        user.setIsEmailVerified(false);
        user.setVerificationToken(emailService.generateVerificationToken());
        
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }
    
    public Optional<UserDTO> updateUser(String id, User userDetails) {
        Long idLong = Long.parseLong(id);
        return userRepository.findById(idLong)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    user.setUniversityDomain(userDetails.getUniversityDomain());
                    return convertToDTO(userRepository.save(user));
                });
    }
    
    public boolean deleteUser(String id) {
        Long idLong = Long.parseLong(id);
        if (userRepository.existsById(idLong)) {
            userRepository.deleteById(idLong);
            return true;
        }
        return false;
    }
    
    public Optional<UserDTO> addPointsToUser(String id, int points) {
        Long idLong = Long.parseLong(id);
        return userRepository.findById(idLong)
                .map(user -> {
                    user.setPoints(user.getPoints() + points);
                    return convertToDTO(userRepository.save(user));
                });
    }
    
    public Optional<UserDTO> addBadgeToUser(String id, BadgeType badge) {
        Long idLong = Long.parseLong(id);
        return userRepository.findById(idLong)
                .map(user -> {
                    user.getBadges().add(badge);
                    return convertToDTO(userRepository.save(user));
                });
    }
    
    public boolean verifyEmail(String verificationToken) {
        Optional<User> userOpt = userRepository.findByVerificationToken(verificationToken);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsEmailVerified(true);
            user.setVerificationToken(null); // Clear the token after verification
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    public User updateUserEntity(User user) {
        return userRepository.save(user);
    }
    
    public boolean sendVerificationCode(String email) {
        try {
            User user = getUserEntityByEmail(email);
            if (user == null) {
                // Create temporary user for verification
                user = createTemporaryUser(email);
            }
            
            // Generate secure 6-digit code
            SecureRandom secureRandom = new SecureRandom();
            String verificationCode = String.format("%06d", secureRandom.nextInt(1000000));
            
            // Set expiry time (10 minutes from now)
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);
            
            // Update user with verification code
            user.setVerificationCode(verificationCode);
            user.setVerificationCodeExpiry(expiryTime);
            userRepository.save(user);
            
            // Log the verification code to the server console so it can be used even if email fails
            System.out.println("==================================================");
            System.out.println("VERIFICATION CODE FOR " + email + ": " + verificationCode);
            System.out.println("==================================================");
            
            // Send email with verification code
            try {
                emailService.sendVerificationCode(user.getEmail(), verificationCode);
                return true;
            } catch (Exception emailException) {
                // Log email error and fail the operation so frontend knows the truth
                System.out.println("CRITICAL EMAIL ERROR: " + emailException.getMessage());
                throw new RuntimeException("Failed to connect to Gmail: " + emailException.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error in sendVerificationCode: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean verifyCode(String email, String code) {
        try {
            User user = getUserEntityByEmail(email);
            if (user == null) {
                return false;
            }
            
            // Check if code matches and is not expired
            if (user.getVerificationCode() != null && 
                user.getVerificationCode().equals(code) &&
                user.getVerificationCodeExpiry() != null &&
                LocalDateTime.now().isBefore(user.getVerificationCodeExpiry())) {
                
                // Clear verification code but don't mark as verified yet
                // This will be done in complete registration
                user.setVerificationCode(null);
                user.setVerificationCodeExpiry(null);
                userRepository.save(user);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private User createTemporaryUser(String email) {
        User tempUser = new User();
        tempUser.setEmail(email);
        tempUser.setUniversityDomain(email.split("@")[1]);
        tempUser.setName("TEMP_USER"); // Temporary name to satisfy validation
        tempUser.setPassword("TEMP_PASSWORD"); // Temporary password to satisfy validation
        tempUser.setIsEmailVerified(false);
        return userRepository.save(tempUser);
    }
    
    public UserDTO updateUserForRegistration(User user) {
        // Hash the password
        user.setPassword(authService.hashPassword(user.getPassword()));
        
        // Generate verification token
        user.setVerificationToken(emailService.generateVerificationToken());
        
        // Save the updated user
        User savedUser = userRepository.save(user);
        
        return convertToDTO(savedUser);
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId().toString());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setUniversityDomain(user.getUniversityDomain());
        dto.setPoints(user.getPoints());
        dto.setIsEmailVerified(user.getIsEmailVerified());
        
        // Convert badges to string set
        Set<String> badgeNames = user.getBadges().stream()
                .map(BadgeType::name)
                .collect(Collectors.toSet());
        dto.setBadges(badgeNames);
        
        return dto;
    }
}
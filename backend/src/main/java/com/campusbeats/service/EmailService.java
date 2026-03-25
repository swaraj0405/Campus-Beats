package com.campusbeats.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    
    @Value("${app.email.from:paramjitbaral44@gmail.com}")
    private String fromEmail;
    
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            logger.info("Attempting to send verification email to: {}", toEmail);
            logger.info("Using email configuration - Host: smtp.gmail.com, From: {}", fromEmail);
            
            // Check if email configuration is properly set
            if ("your-email@gmail.com".equals(fromEmail)) {
                logger.error("Email configuration not properly set. Please configure EMAIL_USERNAME and EMAIL_PASSWORD environment variables.");
                throw new RuntimeException("Email service not configured. Please set up EMAIL_USERNAME and EMAIL_PASSWORD environment variables.");
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Campus Beats - Email Verification");
            
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
            
            String emailBody = "Welcome to Campus Beats!\n\n" +
                    "Please click the link below to verify your email address:\n" +
                    verificationUrl + "\n\n" +
                    "If you didn't create an account with Campus Beats, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Campus Beats Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Campus Beats - Password Reset");
            
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            
            String emailBody = "You have requested to reset your password for Campus Beats.\n\n" +
                    "Please click the link below to reset your password:\n" +
                    resetUrl + "\n\n" +
                    "If you didn't request a password reset, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Campus Beats Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }
    
    public void sendVerificationCode(String toEmail, String verificationCode) {
        try {
            logger.info("Attempting to send verification code to: {}", toEmail);
            
            // Check if email configuration is properly set
            if ("your-email@gmail.com".equals(fromEmail)) {
                logger.warn("Email configuration not properly set. Skipping email send for development. Please configure EMAIL_USERNAME and EMAIL_PASSWORD environment variables for production.");
                logger.info("Verification code for {} would be: {}", toEmail, verificationCode);
                return; // Skip sending email in development
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Campus Beats - Email Verification Code");
            
            String emailBody = "Welcome to Campus Beats!\n\n" +
                    "Your email verification code is: " + verificationCode + "\n\n" +
                    "This code will expire in 10 minutes.\n\n" +
                    "If you didn't create an account with Campus Beats, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Campus Beats Team";
            
            message.setText(emailBody);
            
            mailSender.send(message);
            logger.info("Verification code sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send verification code to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification code: " + e.getMessage());
        }
    }
}
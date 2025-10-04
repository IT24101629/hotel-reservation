package com.hotelreservationsystem.hotelreservationsystem.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for "password"
        String password = "password";
        String hash = encoder.encode(password);
        
        System.out.println("==========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("==========================================");
        
        // Test verification
        boolean matches = encoder.matches(password, hash);
        System.out.println("Hash verification: " + matches);
        
        // Test with the hash from database
        String dbHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        boolean dbMatches = encoder.matches(password, dbHash);
        System.out.println("Database hash matches 'password': " + dbMatches);
        
        // Try other common passwords
        String[] testPasswords = {"password", "123456", "admin", "receptionist", "staff"};
        for (String testPwd : testPasswords) {
            boolean testMatch = encoder.matches(testPwd, dbHash);
            System.out.println("'" + testPwd + "' matches database hash: " + testMatch);
        }
    }
}


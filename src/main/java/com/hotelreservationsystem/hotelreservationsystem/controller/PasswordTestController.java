package com.hotelreservationsystem.hotelreservationsystem.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PasswordTestController {

    @GetMapping("/test/password")
    public Map<String, Object> testPassword(@RequestParam(defaultValue = "password") String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        Map<String, Object> result = new HashMap<>();
        String newHash = encoder.encode(password);
        
        result.put("password", password);
        result.put("newHash", newHash);
        
        // Test current database hash
        String dbHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
        result.put("dbHash", dbHash);
        result.put("dbHashMatches", encoder.matches(password, dbHash));
        result.put("newHashMatches", encoder.matches(password, newHash));
        
        return result;
    }
    
    @GetMapping("/test/hash")
    public Map<String, String> generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        Map<String, String> hashes = new HashMap<>();
        hashes.put("password", encoder.encode("password"));
        hashes.put("123456", encoder.encode("123456"));
        hashes.put("staff", encoder.encode("staff"));
        hashes.put("receptionist", encoder.encode("receptionist"));
        
        return hashes;
    }
}
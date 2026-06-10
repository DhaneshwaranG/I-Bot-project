package com.ibot.controller;

import com.ibot.dto.response.ApiResponse;
import com.ibot.entity.User;
import com.ibot.exception.ResourceNotFoundException;
import com.ibot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<String, Object> data = Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "department", user.getDepartment() != null ? user.getDepartment() : "",
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "role", user.getRole()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMe(
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updates.containsKey("name"))       user.setName(updates.get("name"));
        if (updates.containsKey("phone"))      user.setPhone(updates.get("phone"));
        if (updates.containsKey("department")) user.setDepartment(updates.get("department"));

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", Map.of("name", user.getName())));
    }
}

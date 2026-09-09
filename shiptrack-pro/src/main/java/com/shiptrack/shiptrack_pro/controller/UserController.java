package com.shiptrack.shiptrack_pro.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import com.shiptrack.shiptrack_pro.dto.UserResponse;
import com.shiptrack.shiptrack_pro.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserResponse> getUserProfile(
            @PathVariable Long id,
            Authentication authentication) {

        System.out.println("JWT USER: " + authentication.getName());

        UserResponse user = userService.getUserProfile(id);

        System.out.println("PROFILE USER: " + user.getEmail());

        if (!user.getEmail().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(user);
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return ResponseEntity.ok(
                userService.updateUserStatus(id, status)
        );
    }
}

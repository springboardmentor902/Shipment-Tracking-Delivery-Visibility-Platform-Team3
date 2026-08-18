package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ChangePasswordRequest;
import com.shiptrack.shiptrack_pro.dto.ProfileUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.UserActivityResponse;
import com.shiptrack.shiptrack_pro.dto.UserResponse;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.service.UserActivityService;
import com.shiptrack.shiptrack_pro.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserActivityService userActivityService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(request));
    }

    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeCurrentUserPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserActivityResponse>> getMyActivity(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userActivityService.getForUser(userId, pageable));
    }
}

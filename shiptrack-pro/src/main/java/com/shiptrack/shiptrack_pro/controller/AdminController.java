package com.shiptrack.shiptrack_pro.controller;
 
import com.shiptrack.shiptrack_pro.dto.RoleUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.UserActivityResponse;
import com.shiptrack.shiptrack_pro.dto.UserResponse;
import com.shiptrack.shiptrack_pro.service.UserActivityService;
import com.shiptrack.shiptrack_pro.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
 
    private final UserService userService;
    private final UserActivityService userActivityService;
 
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
 
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                         @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole()));
    }

    @GetMapping("/users/{id}/activity")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<UserActivityResponse>> getUserActivity(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(userActivityService.getForUser(id, pageable));
    }
}

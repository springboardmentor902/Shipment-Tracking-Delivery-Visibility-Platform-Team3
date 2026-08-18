package com.shiptrack.shiptrack_pro.service.impl;
 
import com.shiptrack.shiptrack_pro.dto.*;
import com.shiptrack.shiptrack_pro.entity.PasswordResetToken;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.PasswordResetTokenRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.CurrentUserService;
import com.shiptrack.shiptrack_pro.security.JwtUtil;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.UserActivityService;
import com.shiptrack.shiptrack_pro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
 
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
 
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
 
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CurrentUserService currentUserService;
    private final UserActivityService userActivityService;
 
    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered: " + request.getEmail());
        }
 
        Role requestedRole;
        try {
            requestedRole = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role: " + request.getRole() + ". Must be one of: " +
                            Arrays.toString(Role.values()));
        }
 
        if (requestedRole == Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be created through registration.");
        }
 
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(requestedRole.name())
                .status("ACTIVE")
                .build();
 
        User savedUser = userRepository.save(user);
        userActivityService.log(savedUser, "REGISTER", "Account registered");
        return mapToResponse(savedUser);
    }
 
    @Override
    @Transactional
    public LoginResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));
 
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
 
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account is not active. Current status: " + user.getStatus());
        }
 
        user.setLastLoginAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        userActivityService.log(updatedUser, "LOGIN", "User logged in");
 
        String token = jwtUtil.generateToken(updatedUser.getEmail(), updatedUser.getRole());
 
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToResponse(updatedUser))
                .build();
    }
 
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
 
    @Override
    @Transactional
    public UserResponse updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));
 
        Role role;
        try {
            role = Role.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role: " + newRole + ". Must be one of: " +
                            Arrays.toString(Role.values()));
        }
 
        if (role == Role.ADMINISTRATOR && userRepository.existsByRole("ADMINISTRATOR")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An administrator account already exists. Only one administrator is allowed.");
        }
 
        user.setRole(role.name());
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return mapToResponse(currentUserService.getCurrentUser());
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUserProfile(ProfileUpdateRequest request) {
        User user = currentUserService.getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        userActivityService.log(updatedUser, "PROFILE_UPDATED", "Profile updated");
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(ChangePasswordRequest request) {
        User user = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User updatedUser = userRepository.save(user);
        userActivityService.log(updatedUser, "PASSWORD_CHANGED", "Password changed");
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String message = "If the email exists, a password reset link has been sent.";
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            return ForgotPasswordResponse.builder().message(message).build();
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        passwordResetTokenRepository.save(resetToken);

        System.out.println("Password reset link: /reset-password?token=" + token);

        // only for dev until email sending is added
        return ForgotPasswordResponse.builder()
                .message(message)
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid password reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        userActivityService.log(user, "PASSWORD_RESET", "Password reset completed");
    }
 
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.UserActivityResponse;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.entity.UserActivity;
import com.shiptrack.shiptrack_pro.repository.UserActivityRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void log(User user, String action, String detail) {
        UserActivity activity = UserActivity.builder()
                .user(user)
                .action(action)
                .detail(detail)
                .build();
        userActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityResponse> getForUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }
        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityResponse> getAll(Pageable pageable) {
        return userActivityRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    private UserActivityResponse toResponse(UserActivity activity) {
        return UserActivityResponse.builder()
                .id(activity.getId())
                .userId(activity.getUser().getId())
                .action(activity.getAction())
                .detail(activity.getDetail())
                .ipAddress(activity.getIpAddress())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}

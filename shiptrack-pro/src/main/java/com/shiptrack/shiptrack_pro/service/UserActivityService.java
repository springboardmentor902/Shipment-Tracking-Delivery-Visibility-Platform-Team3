package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.UserActivityResponse;
import com.shiptrack.shiptrack_pro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserActivityService {
    void log(User user, String action, String detail);
    Page<UserActivityResponse> getForUser(Long userId, Pageable pageable);
    Page<UserActivityResponse> getAll(Pageable pageable);
}

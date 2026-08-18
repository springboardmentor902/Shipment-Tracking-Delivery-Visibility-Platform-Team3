package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<UserActivity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

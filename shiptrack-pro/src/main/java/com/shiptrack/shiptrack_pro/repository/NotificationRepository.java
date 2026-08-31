package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Backs the cooldown: has this exact alert already gone out recently? */
    boolean existsByRecipientIdAndDedupeKeyAndCreatedAtAfter(Long recipientId, String dedupeKey,
                                                             LocalDateTime since);

    @Modifying
    @Query("""
           UPDATE Notification n SET n.read = true, n.readAt = :now
           WHERE n.recipient.id = :recipientId AND n.read = false
           """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);
}

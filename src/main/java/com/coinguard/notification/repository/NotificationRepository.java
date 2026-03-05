package com.coinguard.notification.repository;

import com.coinguard.notification.entity.Notification;
import com.coinguard.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Notification> findAllByUserAndReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsReadByUser(User user);
}
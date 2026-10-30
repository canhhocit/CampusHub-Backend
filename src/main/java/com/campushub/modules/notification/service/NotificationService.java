package com.campushub.modules.notification.service;

import com.campushub.common.exception.AppException;
import com.campushub.common.exception.ErrorCode;
import com.campushub.modules.notification.dto.NotificationResponse;
import com.campushub.modules.notification.entity.Notification;
import com.campushub.modules.notification.repository.NotificationRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(User user, String title, String message, String type, String referenceType, Long referenceId) {
        if (user == null) return;
        try {
            Notification notification = new Notification(user, title, message, type, referenceType, referenceId);
            notificationRepository.save(notification);
            log.debug("Created notification [{}] for user #{}", type, user.getId());
        } catch (Exception e) {
            log.error("Failed to save notification for user #{}: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND, "Không tìm thấy thông báo"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Transactional
    public int markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.markAllAsReadByUserId(user.getId());
    }

    private NotificationResponse mapToResponse(Notification n) {
        NotificationResponse res = new NotificationResponse();
        res.setId(n.getId());
        res.setTitle(n.getTitle());
        res.setMessage(n.getMessage());
        res.setType(n.getType());
        res.setReferenceType(n.getReferenceType());
        res.setReferenceId(n.getReferenceId());
        res.setRead(n.isRead());
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }
}


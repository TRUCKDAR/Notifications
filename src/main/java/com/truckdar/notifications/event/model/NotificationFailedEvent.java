package com.truckdar.notifications.event.model;

import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFailedEvent {
    private UUID notificationId;
    private UUID recipientUserId;
    private NotificationChannel channel;
    private NotificationCategory category;
    private String sourceEventId;
    private String sourceEventType;
    private String failureReason;
    private int attempts;
    private Instant failedAt;
}

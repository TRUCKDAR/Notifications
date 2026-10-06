package com.truckdar.notifications.repository;

import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;
import com.truckdar.notifications.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCategoryAndLanguageAndChannel(
            NotificationCategory category,
            String language,
            NotificationChannel channel
    );

    Optional<NotificationTemplate> findByCategoryAndChannel(
            NotificationCategory category,
            NotificationChannel channel
    );
}

package com.truckdar.notifications.service;

import com.truckdar.notifications.dto.request.NotificationTemplateUpdateRequest;
import com.truckdar.notifications.dto.response.NotificationTemplateResponse;
import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationTemplateService {

    List<NotificationTemplateResponse> getAllTemplates();

    NotificationTemplateResponse getTemplateById(UUID id);

    NotificationTemplateResponse updateTemplate(UUID id, NotificationTemplateUpdateRequest request);

    String resolveTemplateText(NotificationCategory category, NotificationChannel channel, String language, Map<String, Object> variables);
}

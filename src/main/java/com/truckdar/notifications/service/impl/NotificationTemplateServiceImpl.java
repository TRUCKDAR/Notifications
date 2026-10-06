package com.truckdar.notifications.service.impl;

import com.truckdar.notifications.dto.request.NotificationTemplateUpdateRequest;
import com.truckdar.notifications.dto.response.NotificationTemplateResponse;
import com.truckdar.notifications.exception.ResourceNotFoundException;
import com.truckdar.notifications.mapper.NotificationTemplateMapper;
import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;
import com.truckdar.notifications.model.NotificationTemplate;
import com.truckdar.notifications.repository.NotificationTemplateRepository;
import com.truckdar.notifications.service.NotificationTemplateService;
import com.truckdar.notifications.template.TemplateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationTemplateMapper templateMapper;
    private final TemplateResolver templateResolver;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateMapper.toResponseList(templateRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateById(UUID id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template not found with id: " + id));
        return templateMapper.toResponse(template);
    }

    @Override
    @Transactional
    public NotificationTemplateResponse updateTemplate(UUID id, NotificationTemplateUpdateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template not found with id: " + id));

        template.setTemplateText(request.getTemplateText());
        NotificationTemplate updated = templateRepository.save(template);
        log.info("Updated notification template: id={}, category={}, channel={}", id, updated.getCategory(), updated.getChannel());
        return templateMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveTemplateText(NotificationCategory category, NotificationChannel channel, String language, Map<String, Object> variables) {
        String effectiveLang = (language != null && !language.isBlank()) ? language : "es-CO";

        NotificationTemplate template = templateRepository.findByCategoryAndLanguageAndChannel(category, effectiveLang, channel)
                .or(() -> templateRepository.findByCategoryAndChannel(category, channel))
                .orElse(null);

        if (template == null) {
            log.warn("No template found for category={}, channel={}, language={}. Using default fallback representation.",
                    category, channel, effectiveLang);
            return defaultFallbackText(category, variables);
        }

        return templateResolver.resolve(template.getTemplateText(), variables);
    }

    private String defaultFallbackText(NotificationCategory category, Map<String, Object> variables) {
        if (variables != null && variables.containsKey("message")) {
            return String.valueOf(variables.get("message"));
        }
        return "Notificación de TruckDar (" + category + ")";
    }
}

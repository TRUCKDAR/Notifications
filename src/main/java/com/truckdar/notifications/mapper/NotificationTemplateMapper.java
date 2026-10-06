package com.truckdar.notifications.mapper;

import com.truckdar.notifications.dto.response.NotificationTemplateResponse;
import com.truckdar.notifications.model.NotificationTemplate;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationTemplateMapper {

    NotificationTemplateResponse toResponse(NotificationTemplate template);

    List<NotificationTemplateResponse> toResponseList(List<NotificationTemplate> templates);
}

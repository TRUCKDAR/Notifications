package com.truckdar.notifications.mapper;

import com.truckdar.notifications.dto.response.NotificationResponse;
import com.truckdar.notifications.model.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}

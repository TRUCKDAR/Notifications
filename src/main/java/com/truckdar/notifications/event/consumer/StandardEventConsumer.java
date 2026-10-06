package com.truckdar.notifications.event.consumer;

import com.truckdar.notifications.event.model.DispatcherMessageEvent;
import com.truckdar.notifications.event.model.LoadReassignedEvent;
import com.truckdar.notifications.event.model.TripStatusChangedEvent;
import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;
import com.truckdar.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for standard domain events (load reassignment, trip status updates, dispatcher direct messages).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StandardEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.load-reassigned:truckdar.loads.reassigned}",
            groupId = "${spring.kafka.consumer.group-id:notifications-group}",
            containerFactory = "standardKafkaListenerContainerFactory"
    )
    public void consumeLoadReassigned(LoadReassignedEvent event) {
        log.info("STANDARD EVENT received: LoadReassignedEvent id='{}', loadId='{}' for user='{}'",
                event.getEventId(), event.getLoadId(), event.getRecipientUserId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("loadId", event.getLoadId());
        vars.put("ruta", event.getRoute());
        vars.put("route", event.getRoute());
        vars.put("origin", event.getOrigin());
        vars.put("destination", event.getDestination());
        vars.put("reason", event.getReason());

        notificationService.processEventNotification(
                event.getRecipientUserId(),
                NotificationCategory.REASIGNACION_CARGA,
                NotificationChannel.PUSH,
                event.getEventId(),
                "LoadReassignedEvent",
                "es-CO",
                vars,
                null
        );
    }

    @KafkaListener(
            topics = "${app.kafka.topics.trip-status:truckdar.trips.status}",
            groupId = "${spring.kafka.consumer.group-id:notifications-group}",
            containerFactory = "standardKafkaListenerContainerFactory"
    )
    public void consumeTripStatusChanged(TripStatusChangedEvent event) {
        log.info("STANDARD EVENT received: TripStatusChangedEvent id='{}', tripId='{}', status='{}' for user='{}'",
                event.getEventId(), event.getTripId(), event.getStatus(), event.getRecipientUserId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("tripId", event.getTripId());
        vars.put("status", event.getStatus());
        vars.put("location", event.getLocation());

        notificationService.processEventNotification(
                event.getRecipientUserId(),
                NotificationCategory.CAMBIO_ESTADO_VIAJE,
                NotificationChannel.PUSH,
                event.getEventId(),
                "TripStatusChangedEvent",
                "es-CO",
                vars,
                null
        );
    }

    @KafkaListener(
            topics = "${app.kafka.topics.dispatcher-message:truckdar.dispatch.messages}",
            groupId = "${spring.kafka.consumer.group-id:notifications-group}",
            containerFactory = "standardKafkaListenerContainerFactory"
    )
    public void consumeDispatcherMessage(DispatcherMessageEvent event) {
        log.info("STANDARD EVENT received: DispatcherMessageEvent id='{}', from='{}' for user='{}'",
                event.getEventId(), event.getDispatcherName(), event.getRecipientUserId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("dispatcherName", event.getDispatcherName());
        vars.put("message", event.getMessage());

        // Dispatcher messages can be delivered via Push and/or Voice according to configuration/event
        if (event.isSendPush()) {
            notificationService.processEventNotification(
                    event.getRecipientUserId(),
                    NotificationCategory.MENSAJE_DESPACHADOR,
                    NotificationChannel.PUSH,
                    event.getEventId() + "-push",
                    "DispatcherMessageEvent",
                    "es-CO",
                    vars,
                    null
            );
        }

        if (event.isSendVoice()) {
            notificationService.processEventNotification(
                    event.getRecipientUserId(),
                    NotificationCategory.MENSAJE_DESPACHADOR,
                    NotificationChannel.VOICE,
                    event.getEventId() + "-voice",
                    "DispatcherMessageEvent",
                    "es-CO",
                    vars,
                    null
            );
        }
    }
}

package com.truckdar.notifications.event.consumer;

import com.truckdar.notifications.channel.PushNotifier;
import com.truckdar.notifications.channel.VoiceSynthesizer;
import com.truckdar.notifications.channel.push.PushNotificationResult;
import com.truckdar.notifications.channel.voice.VoiceSynthesisResult;
import com.truckdar.notifications.event.model.LoadReassignedEvent;
import com.truckdar.notifications.event.model.RouteAlertConfirmedEvent;
import com.truckdar.notifications.model.Notification;
import com.truckdar.notifications.model.NotificationCategory;
import com.truckdar.notifications.model.NotificationChannel;
import com.truckdar.notifications.model.NotificationStatus;
import com.truckdar.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "truckdar.route.alerts",
        "truckdar.route.closures",
        "truckdar.loads.reassigned",
        "truckdar.trips.status",
        "truckdar.dispatch.messages"
})
class EventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.truckdar.notifications.repository.NotificationTemplateRepository templateRepository;

    @MockBean
    private PushNotifier pushNotifier;

    @MockBean
    private VoiceSynthesizer voiceSynthesizer;

    @MockBean
    private SnsClient snsClient;

    @MockBean
    private PollyClient pollyClient;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;

    @org.junit.jupiter.api.BeforeEach
    void setUpTemplates() {
        if (templateRepository.findByCategoryAndChannel(NotificationCategory.REASIGNACION_CARGA, NotificationChannel.PUSH).isEmpty()) {
            templateRepository.save(com.truckdar.notifications.model.NotificationTemplate.builder()
                    .category(NotificationCategory.REASIGNACION_CARGA)
                    .language("es-CO")
                    .channel(NotificationChannel.PUSH)
                    .templateText("Tu carga {{loadId}} ha sido reasignada a la ruta {{ruta}}. Motivo: {{reason}}.")
                    .build());
        }
        if (templateRepository.findByCategoryAndChannel(NotificationCategory.ALERTA_RUTA, NotificationChannel.VOICE).isEmpty()) {
            templateRepository.save(com.truckdar.notifications.model.NotificationTemplate.builder()
                    .category(NotificationCategory.ALERTA_RUTA)
                    .language("es-CO")
                    .channel(NotificationChannel.VOICE)
                    .templateText("Atención conductor. Se reporta {{alertType}} en {{roadSegment}}. {{description}}.")
                    .build());
        }
    }

    @Test
    @DisplayName("Should consume LoadReassignedEvent and record PUSH notification")
    void shouldConsumeLoadReassignedEvent() {
        UUID userId = UUID.randomUUID();
        String eventId = "evt-load-int-" + UUID.randomUUID();

        when(pushNotifier.sendPush(eq(userId), anyString(), anyString(), anyMap()))
                .thenReturn(PushNotificationResult.ok("sns-int-123"));

        LoadReassignedEvent event = LoadReassignedEvent.builder()
                .eventId(eventId)
                .recipientUserId(userId)
                .loadId("CRG-555")
                .route("Medellín - Cartagena")
                .origin("Medellín")
                .destination("Cartagena")
                .reason("Avería mecánica en tractomula previa")
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send("truckdar.loads.reassigned", userId.toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Notification> notification = notificationRepository
                    .findBySourceEventIdAndChannel(eventId, NotificationChannel.PUSH);
            assertThat(notification).isPresent();
            assertThat(notification.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notification.get().getCategory()).isEqualTo(NotificationCategory.REASIGNACION_CARGA);
            assertThat(notification.get().getPayload()).contains("CRG-555");
        });
    }

    @Test
    @DisplayName("Should consume RouteAlertConfirmedEvent and record VOICE notification")
    void shouldConsumeRouteAlertConfirmedEvent() {
        UUID userId = UUID.randomUUID();
        String eventId = "evt-alert-int-" + UUID.randomUUID();

        when(voiceSynthesizer.synthesizeAndUpload(anyString(), anyString()))
                .thenReturn(VoiceSynthesisResult.ok("https://s3.amazonaws.com/test-bucket/alert.mp3", "audio/alert.mp3", false));

        RouteAlertConfirmedEvent event = RouteAlertConfirmedEvent.builder()
                .eventId(eventId)
                .recipientUserId(userId)
                .alertType("Derrumbe activo")
                .roadSegment("Vía al Llano km 58")
                .description("Cierre preventivo de carril")
                .severity("HIGH")
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send("truckdar.route.alerts", userId.toString(), event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Notification> notification = notificationRepository
                    .findBySourceEventIdAndChannel(eventId, NotificationChannel.VOICE);
            assertThat(notification).isPresent();
            assertThat(notification.get().getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(notification.get().getCategory()).isEqualTo(NotificationCategory.ALERTA_RUTA);
            assertThat(notification.get().getAudioUrl()).isEqualTo("https://s3.amazonaws.com/test-bucket/alert.mp3");
        });
    }
}

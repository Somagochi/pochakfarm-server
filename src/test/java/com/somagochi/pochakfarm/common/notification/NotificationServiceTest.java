package com.somagochi.pochakfarm.common.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

  @Test
  void routesNotificationToSupportingNotifier() {
    AtomicReference<Notification> delivered = new AtomicReference<>();
    Notifier smsNotifier =
        new Notifier() {
          @Override
          public boolean supports(Notification notification) {
            return notification instanceof SmsNotification;
          }

          @Override
          public NotificationResult notify(Notification notification) {
            delivered.set(notification);
            return NotificationResult.success();
          }
        };
    NotificationService service = new NotificationService(List.of(smsNotifier));
    SmsNotification notification = new SmsNotification("01012345678", "text");

    service.notify(notification);

    assertEquals(notification, delivered.get());
  }

  @Test
  void throwsWhenNoNotifierSupportsNotification() {
    NotificationService service = new NotificationService(List.of());

    assertThrows(
        IllegalArgumentException.class,
        () -> service.notify(new SmsNotification("01012345678", "text")));
  }
}

package com.somagochi.pochakfarm.common.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationService {

  private final List<Notifier> notifiers;

  public NotificationResult notify(Notification notification) {
    return notifiers.stream()
        .filter(notifier -> notifier.supports(notification))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No notifier supports " + notification.getClass().getSimpleName()))
        .notify(notification);
  }
}

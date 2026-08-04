package com.somagochi.pochakfarm.common.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationService {

  private final List<Notifier> notifiers;

  public void notify(Notification notification) {
    notifiers.stream()
        .filter(notifier -> notifier.supports(notification))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No notifier supports " + notification.getClass().getSimpleName()))
        .notify(notification);
  }
}

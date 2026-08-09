package com.somagochi.pochakfarm.common.notification;

import java.util.List;

public record NotificationResult(List<String> failedRecipients) {

  public static NotificationResult success() {
    return new NotificationResult(List.of());
  }
}

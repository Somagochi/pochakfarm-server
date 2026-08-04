package com.somagochi.pochakfarm.common.notification;

public interface Notifier {

  boolean supports(Notification notification);

  NotificationResult notify(Notification notification);
}

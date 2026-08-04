package com.somagochi.pochakfarm.common.notification;

public interface Notifier {

  boolean supports(Notification notification);

  void notify(Notification notification);
}

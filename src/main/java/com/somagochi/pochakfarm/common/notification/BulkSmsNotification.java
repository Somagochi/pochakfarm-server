package com.somagochi.pochakfarm.common.notification;

import java.util.List;

public record BulkSmsNotification(List<SmsNotification> messages) implements Notification {}

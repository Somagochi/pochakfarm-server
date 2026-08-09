package com.somagochi.pochakfarm.common.notification;

public record SmsNotification(String to, String text) implements Notification {}

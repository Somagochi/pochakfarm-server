package com.somagochi.pochakfarm.common.notification.sms;

public record SolapiSendRequest(SolapiMessage message) {

  public static SolapiSendRequest of(String to, String from, String text) {
    return new SolapiSendRequest(new SolapiMessage(to, from, text));
  }

  public record SolapiMessage(String to, String from, String text) {}
}

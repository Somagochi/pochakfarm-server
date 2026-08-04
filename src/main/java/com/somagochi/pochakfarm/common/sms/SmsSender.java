package com.somagochi.pochakfarm.common.sms;

public interface SmsSender {

  void send(String to, String text);
}

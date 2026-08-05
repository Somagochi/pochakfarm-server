package com.somagochi.pochakfarm.common.notification.sms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SolapiSendManyResponse(List<FailedMessage> failedMessageList) {

  public List<FailedMessage> failures() {
    return failedMessageList == null ? List.of() : failedMessageList;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FailedMessage(String to, String statusCode, String statusMessage) {}
}

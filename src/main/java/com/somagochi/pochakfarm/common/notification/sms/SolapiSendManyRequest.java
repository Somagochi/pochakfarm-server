package com.somagochi.pochakfarm.common.notification.sms;

import java.util.List;

public record SolapiSendManyRequest(List<SolapiSendRequest.SolapiMessage> messages) {}

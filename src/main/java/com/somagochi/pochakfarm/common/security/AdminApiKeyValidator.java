package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.AdminApiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AdminApiKeyValidator {

  private final AdminApiProperties properties;

  public void validate(String providedKey) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ADMIN_ACCESS);
    }
    byte[] configured = properties.apiKey().getBytes(StandardCharsets.UTF_8);
    byte[] provided = (providedKey == null ? "" : providedKey).getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(configured, provided)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ADMIN_ACCESS);
    }
  }
}

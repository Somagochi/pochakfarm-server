package com.somagochi.pochakfarm.common.social;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.Locale;

public enum SocialProvider {
  KAKAO,
  NAVER;

  public static SocialProvider from(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }
    try {
      return SocialProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }
  }
}

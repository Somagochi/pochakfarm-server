package com.somagochi.pochakfarm.common.security;

import com.somagochi.pochakfarm.common.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {

  private final ErrorCode errorCode;

  public JwtAuthenticationException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public int getStatus() {
    return errorCode.getStatus();
  }

  public String getCode() {
    return errorCode.getCode();
  }

  public String getErrorMessage() {
    return errorCode.getMessage();
  }
}

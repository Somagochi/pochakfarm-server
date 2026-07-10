package com.somagochi.pochakfarm.common.exception;

public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public int getStatus() {
    return errorCode.getStatus();
  }

  public String getCode() {
    return errorCode.getCode();
  }

  public String getMessage() {
    return errorCode.getMessage();
  }
}

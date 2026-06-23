package com.somagochi.pochakfarm.common.exception;

public enum ErrorCode {
  INVALID_TOKEN(401, "INVALID_TOKEN", "Invalid token"),
  EXPIRED_TOKEN(401, "EXPIRED_TOKEN", "Token has expired"),
  INVALID_TOKEN_TYPE(401, "INVALID_TOKEN_TYPE", "Invalid token type"),
  BLACKLISTED_TOKEN(401, "BLACKLISTED_TOKEN", "Blacklisted token"),
  REVOKED_REFRESH_TOKEN(401, "REVOKED_REFRESH_TOKEN", "Revoked refresh token"),
  UNSUPPORTED_SOCIAL_PROVIDER(400, "UNSUPPORTED_SOCIAL_PROVIDER", "Unsupported social provider"),
  INVALID_SOCIAL_TOKEN(401, "INVALID_SOCIAL_TOKEN", "Invalid social token"),
  SOCIAL_USER_INFO_FAILED(502, "SOCIAL_USER_INFO_FAILED", "Failed to fetch social user info"),
  USER_NOT_FOUND(404, "USER_NOT_FOUND", "User not found"),
  TOKEN_OWNER_MISMATCH(401, "TOKEN_OWNER_MISMATCH", "Token owner mismatch"),
  ;

  private final int status;
  private final String code;
  private final String message;

  ErrorCode(int status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public int getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}

package com.somagochi.pochakfarm.common.exception;

public enum ErrorCode {
  INVALID_TOKEN(401, "INVALID_TOKEN", "Invalid token"),
  EXPIRED_TOKEN(401, "EXPIRED_TOKEN", "Token has expired"),
  INVALID_TOKEN_TYPE(401, "INVALID_TOKEN_TYPE", "Invalid token type"),
  BLACKLISTED_TOKEN(401, "BLACKLISTED_TOKEN", "Blacklisted token"),
  UNSUPPORTED_SOCIAL_PROVIDER(400, "UNSUPPORTED_SOCIAL_PROVIDER", "Unsupported social provider"),
  INVALID_SOCIAL_TOKEN(401, "INVALID_SOCIAL_TOKEN", "Invalid social token"),
  SOCIAL_USER_INFO_FAILED(502, "SOCIAL_USER_INFO_FAILED", "Failed to fetch social user info"),
  USER_NOT_FOUND(404, "USER_NOT_FOUND", "User not found"),
  TOKEN_OWNER_MISMATCH(401, "TOKEN_OWNER_MISMATCH", "Token owner mismatch"),
  UNSUPPORTED_CONTENT_TYPE(400, "UNSUPPORTED_CONTENT_TYPE", "Unsupported content type"),
  INVALID_UPLOAD_PURPOSE(400, "INVALID_UPLOAD_PURPOSE", "Invalid upload purpose"),
  EMPTY_FILE(400, "EMPTY_FILE", "Empty file"),
  FILE_NOT_FOUND(404, "FILE_NOT_FOUND", "File not found"),
  FILE_TOO_LARGE(413, "FILE_TOO_LARGE", "File too large"),
  FORBIDDEN_FILE_ACCESS(403, "FORBIDDEN_FILE_ACCESS", "Forbidden file access"),
  DEVICE_NOT_FOUND(404, "DEVICE_NOT_FOUND", "Device not found"),
  INVALID_PHONE_NUMBER(400, "INVALID_PHONE_NUMBER", "Invalid phone number"),
  REQUIRED_CONSENT_REQUIRED(400, "REQUIRED_CONSENT_REQUIRED", "Required consent is missing"),
  PHONE_NUMBER_ALREADY_REGISTERED(
      409, "PHONE_NUMBER_ALREADY_REGISTERED", "Phone number already registered"),
  DEVICE_ALREADY_REGISTERED(409, "DEVICE_ALREADY_REGISTERED", "Device already registered"),
  PRE_REGISTRATION_NOT_FOUND(404, "PRE_REGISTRATION_NOT_FOUND", "Pre-registration not found"),
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

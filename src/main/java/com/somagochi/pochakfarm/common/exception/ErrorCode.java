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
  USER_ALREADY_REGISTERED(409, "USER_ALREADY_REGISTERED", "User already registered"),
  INVALID_NICKNAME(400, "INVALID_NICKNAME", "Invalid nickname"),
  TOKEN_OWNER_MISMATCH(401, "TOKEN_OWNER_MISMATCH", "Token owner mismatch"),
  UNSUPPORTED_CONTENT_TYPE(400, "UNSUPPORTED_CONTENT_TYPE", "Unsupported content type"),
  INVALID_UPLOAD_PURPOSE(400, "INVALID_UPLOAD_PURPOSE", "Invalid upload purpose"),
  EMPTY_FILE(400, "EMPTY_FILE", "Empty file"),
  FILE_NOT_FOUND(404, "FILE_NOT_FOUND", "File not found"),
  FILE_TOO_LARGE(413, "FILE_TOO_LARGE", "File too large"),
  FORBIDDEN_FILE_ACCESS(403, "FORBIDDEN_FILE_ACCESS", "Forbidden file access"),
  INVALID_ANIMAL_NAME(400, "INVALID_ANIMAL_NAME", "Invalid animal name"),
  CHARACTERIZATION_NOT_FOUND(404, "CHARACTERIZATION_NOT_FOUND", "Characterization not found"),
  UNSUPPORTED_CHARACTERIZATION_IMAGE(
      422, "UNSUPPORTED_CHARACTERIZATION_IMAGE", "Unsupported characterization image"),
  CHARACTERIZATION_FAILED(502, "CHARACTERIZATION_FAILED", "Failed to characterize image"),
  CHARACTERIZATION_ALREADY_USED(
      409, "CHARACTERIZATION_ALREADY_USED", "Characterization already used"),
  CHARACTERIZATION_ALREADY_PROCESSING(
      409, "CHARACTERIZATION_ALREADY_PROCESSING", "Characterization already processing"),
  CHARACTERIZATION_BUSY(503, "CHARACTERIZATION_BUSY", "Characterization service is busy"),
  CHARACTERIZATION_TIMED_OUT(504, "CHARACTERIZATION_TIMED_OUT", "Characterization timed out"),
  INVALID_CLIENT_REQUEST_ID(400, "INVALID_CLIENT_REQUEST_ID", "Invalid client request id"),
  CAPTURE_ATTEMPT_EXHAUSTED(409, "CAPTURE_ATTEMPT_EXHAUSTED", "Capture attempt exhausted"),
  CAPTURE_REQUEST_CONFLICT(409, "CAPTURE_REQUEST_CONFLICT", "Capture request conflict"),
  CAPTURE_NOT_FOUND(404, "CAPTURE_NOT_FOUND", "Capture not found"),
  FORBIDDEN_CAPTURE_ACCESS(403, "FORBIDDEN_CAPTURE_ACCESS", "Forbidden capture access"),
  INVALID_GAME_RESULT(400, "INVALID_GAME_RESULT", "Invalid game result"),
  DEVICE_NOT_FOUND(404, "DEVICE_NOT_FOUND", "Device not found"),
  INVALID_PHONE_NUMBER(400, "INVALID_PHONE_NUMBER", "Invalid phone number"),
  INVALID_CHARACTERIZATION_ID(400, "INVALID_CHARACTERIZATION_ID", "Invalid characterization id"),
  REQUIRED_CONSENT_REQUIRED(400, "REQUIRED_CONSENT_REQUIRED", "Required consent is missing"),
  PHONE_NUMBER_ALREADY_REGISTERED(
      409, "PHONE_NUMBER_ALREADY_REGISTERED", "Phone number already registered"),
  DEVICE_ALREADY_REGISTERED(409, "DEVICE_ALREADY_REGISTERED", "Device already registered"),
  PRE_REGISTRATION_NOT_FOUND(404, "PRE_REGISTRATION_NOT_FOUND", "Pre-registration not found"),
  INVALID_PARAMETER(400, "INVALID_PARAMETER", "Invalid request parameter"),
  FARM_SPACE_NOT_FOUND(404, "FARM_SPACE_NOT_FOUND", "Farm space not found"),
  ANIMAL_NOT_FOUND(404, "ANIMAL_NOT_FOUND", "Animal not found"),
  FORBIDDEN_ANIMAL_ACCESS(403, "FORBIDDEN_ANIMAL_ACCESS", "Forbidden animal access"),
  FARM_SLOT_NOT_FOUND(404, "FARM_SLOT_NOT_FOUND", "Farm slot not found"),
  ANIMAL_NOT_PLACED(409, "ANIMAL_NOT_PLACED", "Animal is not placed in the farm"),
  CONCURRENCY_CONFLICT(409, "CONCURRENCY_CONFLICT", "Concurrent modification conflict"),
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

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
  APPLE_CLIENT_SECRET_FAILED(
      500, "APPLE_CLIENT_SECRET_FAILED", "Failed to generate Apple client secret"),
  EMAIL_NOT_FOUND(400, "EMAIL_NOT_FOUND", "Email not found"),
  USER_NOT_FOUND(404, "USER_NOT_FOUND", "User not found"),
  USER_ALREADY_REGISTERED(409, "USER_ALREADY_REGISTERED", "User already registered"),
  INVALID_NICKNAME(400, "INVALID_NICKNAME", "Invalid nickname"),
  DUPLICATE_NICKNAME(409, "DUPLICATE_NICKNAME", "Nickname is already in use"),
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
  INSUFFICIENT_COINS(402, "INSUFFICIENT_COINS", "Insufficient coins"),
  CAPTURE_ATTEMPT_REQUIRED(409, "CAPTURE_ATTEMPT_REQUIRED", "Capture attempt is required"),
  CAPTURE_ATTEMPT_ALREADY_AVAILABLE(
      409, "CAPTURE_ATTEMPT_ALREADY_AVAILABLE", "Capture attempt is already available"),
  CAPTURE_REQUEST_CONFLICT(409, "CAPTURE_REQUEST_CONFLICT", "Capture request conflict"),
  CAPTURE_NOT_FOUND(404, "CAPTURE_NOT_FOUND", "Capture not found"),
  FORBIDDEN_CAPTURE_ACCESS(403, "FORBIDDEN_CAPTURE_ACCESS", "Forbidden capture access"),
  CAPTURE_NOT_PLACEABLE(409, "CAPTURE_NOT_PLACEABLE", "Capture is not placeable in farm"),
  CAPTURE_ALREADY_PLACED(409, "CAPTURE_ALREADY_PLACED", "Capture is already placed in farm"),
  CAPTURE_PLACEMENT_CONFLICT(
      409, "CAPTURE_PLACEMENT_CONFLICT", "Capture placement request conflict"),
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
  FARM_SLOT_OCCUPIED(409, "FARM_SLOT_OCCUPIED", "Farm slot is occupied"),
  ANIMAL_REPLACEMENT_CONFLICT(
      409, "ANIMAL_REPLACEMENT_CONFLICT", "Animal replacement target conflict"),
  ANIMAL_NOT_PLACED(409, "ANIMAL_NOT_PLACED", "Animal is not placed in the farm"),
  FARM_FLOOR_MAX_REACHED(409, "FARM_FLOOR_MAX_REACHED", "Farm floor already at maximum"),
  CONCURRENCY_CONFLICT(409, "CONCURRENCY_CONFLICT", "Concurrent modification conflict"),
  FORBIDDEN_FARM_SLOT_ACCESS(403, "FORBIDDEN_FARM_SLOT_ACCESS", "Forbidden farm slot access"),
  FARM_SLOT_TYPE_MISMATCH(400, "FARM_SLOT_TYPE_MISMATCH", "Farm slot type mismatch"),
  FARM_SPACE_FULL(419, "FARM_SPACE_FULL", "Farm space is full"),
  ACHIEVEMENT_NOT_FOUND(404, "ACHIEVEMENT_NOT_FOUND", "Achievement not found"),
  ACHIEVEMENT_NOT_ACHIEVED(400, "ACHIEVEMENT_NOT_ACHIEVED", "Achievement is not achieved yet"),
  ACHIEVEMENT_REWARD_ALREADY_CLAIMED(
      409, "ACHIEVEMENT_REWARD_ALREADY_CLAIMED", "Achievement reward already claimed"),
  BADGE_NOT_FOUND(404, "BADGE_NOT_FOUND", "Badge not found"),
  COUPON_NOT_FOUND(404, "COUPON_NOT_FOUND", "Coupon not found"),
  COUPON_EXPIRED(400, "COUPON_EXPIRED", "Coupon has expired"),
  COUPON_ALREADY_USED(409, "COUPON_ALREADY_USED", "Coupon already used"),
  COUPON_ALREADY_REDEEMED(409, "COUPON_ALREADY_REDEEMED", "User already redeemed a coupon"),
  COUPON_REDEEM_NOT_STARTED(409, "COUPON_REDEEM_NOT_STARTED", "Coupon redeem not started"),
  FORBIDDEN_COUPON_ACCESS(403, "FORBIDDEN_COUPON_ACCESS", "Forbidden coupon access"),
  FORBIDDEN_ADMIN_ACCESS(403, "FORBIDDEN_ADMIN_ACCESS", "Forbidden admin access"),
  BATTLE_NOT_FOUND(404, "BATTLE_NOT_FOUND", "Battle not found"),
  FORBIDDEN_BATTLE_ACCESS(403, "FORBIDDEN_BATTLE_ACCESS", "Forbidden battle access"),
  BATTLE_ALREADY_IN_PROGRESS(409, "BATTLE_ALREADY_IN_PROGRESS", "Battle is already in progress"),
  BATTLE_NOT_IN_PROGRESS(409, "BATTLE_NOT_IN_PROGRESS", "Battle is not in progress"),
  BATTLE_ANIMAL_RESTING(409, "BATTLE_ANIMAL_RESTING", "Animal is resting"),
  INVALID_BATTLE_ENTRY(400, "INVALID_BATTLE_ENTRY", "Invalid battle entry"),
  BATTLE_ACTION_SEQUENCE_MISMATCH(
      409, "BATTLE_ACTION_SEQUENCE_MISMATCH", "Battle action sequence mismatch"),
  BATTLE_ACTION_CONFLICT(409, "BATTLE_ACTION_CONFLICT", "Battle action request conflict"),
  BATTLE_FINAL_ROUND_EXPIRED(409, "BATTLE_FINAL_ROUND_EXPIRED", "Battle final round expired"),
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

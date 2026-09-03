package com.somagochi.pochakfarm.common.exception;

import com.somagochi.pochakfarm.common.security.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String BAD_REQUEST_MESSAGE = "잘못된 요청입니다.";
  private static final String UNAUTHORIZED_MESSAGE = "로그인이 필요합니다.";
  private static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
  private static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "서버 오류가 발생했습니다.";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    logByStatus(exception.getStatus(), exception);
    return buildResponse(exception.getStatus(), exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException exception) {
    logClientError(exception);
    return buildResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", BAD_REQUEST_MESSAGE);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException exception) {
    logClientError(exception);
    return buildResponse(
        ErrorCode.INVALID_PARAMETER.getStatus(),
        ErrorCode.INVALID_PARAMETER.getCode(),
        invalidParameterMessage(exception));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException exception) {
    logClientError(exception);
    return buildResponse(
        ErrorCode.INVALID_PARAMETER.getStatus(),
        ErrorCode.INVALID_PARAMETER.getCode(),
        exception.getParameterName() + "은(는) 필수 값입니다.");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception) {
    logClientError(exception);
    return buildResponse(
        ErrorCode.INVALID_PARAMETER.getStatus(),
        ErrorCode.INVALID_PARAMETER.getCode(),
        ErrorCode.INVALID_PARAMETER.getMessage());
  }

  @ExceptionHandler(JwtAuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(
      JwtAuthenticationException exception) {
    logByStatus(exception.getStatus(), exception);
    return buildResponse(exception.getStatus(), exception.getCode(), exception.getErrorMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException exception, HttpServletRequest request) {
    logAccessError("Authentication required", request, exception);
    return buildResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", UNAUTHORIZED_MESSAGE);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception, HttpServletRequest request) {
    logAccessError("Access denied", request, exception);
    return buildResponse(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", FORBIDDEN_MESSAGE);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException exception) {
    logClientError(exception);
    return buildResponse(
        ErrorCode.FILE_TOO_LARGE.getStatus(),
        ErrorCode.FILE_TOO_LARGE.getCode(),
        ErrorCode.FILE_TOO_LARGE.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
      NoResourceFoundException exception) {
    log.warn("Resource not found: {} {}", exception.getHttpMethod(), exception.getResourcePath());
    return buildResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", NOT_FOUND_MESSAGE);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
      OptimisticLockingFailureException exception) {
    logClientError(exception);
    return buildResponse(
        ErrorCode.CONCURRENCY_CONFLICT.getStatus(),
        ErrorCode.CONCURRENCY_CONFLICT.getCode(),
        ErrorCode.CONCURRENCY_CONFLICT.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    logServerError(exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "INTERNAL_SERVER_ERROR",
        INTERNAL_SERVER_ERROR_MESSAGE);
  }

  private String invalidParameterMessage(MethodArgumentTypeMismatchException exception) {
    Class<?> requiredType = exception.getRequiredType();
    if (requiredType != null && requiredType.isEnum()) {
      String allowed =
          Arrays.stream(requiredType.getEnumConstants())
              .map(constant -> ((Enum<?>) constant).name())
              .collect(Collectors.joining(", "));
      return exception.getName() + "은(는) [" + allowed + "] 중 하나여야 합니다.";
    }
    return exception.getName() + "의 값이 올바르지 않습니다.";
  }

  private void logByStatus(int status, Exception e) {
    if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      logServerError(e);
    } else {
      logClientError(e);
    }
  }

  private void logClientError(Exception e) {
    log.warn("Client error: {}", e.getMessage(), e);
  }

  private void logAccessError(String reason, HttpServletRequest request, Exception e) {
    log.warn(
        "{}: {} {} ({})", reason, request.getMethod(), request.getRequestURI(), e.getMessage());
  }

  private void logServerError(Exception e) {
    log.error("Server error: {}", e.getMessage(), e);
  }

  private ResponseEntity<ErrorResponse> buildResponse(int status, String code, String message) {
    return ResponseEntity.status(HttpStatusCode.valueOf(status))
        .body(ErrorResponse.of(status, code, message));
  }
}

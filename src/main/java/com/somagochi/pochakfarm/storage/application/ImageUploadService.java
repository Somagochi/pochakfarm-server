package com.somagochi.pochakfarm.storage.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.S3Properties;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.domain.PresignedUpload;
import com.somagochi.pochakfarm.storage.domain.StoredObject;
import com.somagochi.pochakfarm.storage.dto.ConfirmResponse;
import com.somagochi.pochakfarm.storage.dto.PresignResponse;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ImageUploadService {

  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
  private static final Pattern PURPOSE_PATTERN = Pattern.compile("^[a-z0-9-]{1,30}$");

  private final FileStorage fileStorage;
  private final S3Properties properties;

  public ImageUploadService(FileStorage fileStorage, S3Properties properties) {
    this.fileStorage = fileStorage;
    this.properties = properties;
  }

  public PresignResponse createPresign(Long userId, String purpose, String contentType) {
    validateContentType(contentType);
    String key = buildKey(userId, purpose, contentType);
    return presign(key, contentType);
  }

  public PresignResponse refreshPresign(Long userId, String key, String contentType) {
    validateOwnership(userId, key);
    validateContentType(contentType);
    return presign(key, contentType);
  }

  public PresignResponse createPublicPresign(String purpose, String contentType) {
    validateContentType(contentType);
    String key = buildPublicKey(purpose, contentType);
    return presign(key, contentType);
  }

  public PresignResponse createDownloadPresign(Long userId, String key) {
    validateOwnership(userId, key);
    PresignedUpload download = fileStorage.presignGet(key, properties.presignExpiration());
    return new PresignResponse(download.url(), key, download.expiresAt());
  }

  private PresignResponse presign(String key, String contentType) {
    PresignedUpload upload =
        fileStorage.presignPut(key, contentType, properties.presignExpiration());
    return new PresignResponse(upload.url(), key, upload.expiresAt());
  }

  public void validateUploadedObject(Long userId, String key, String expectedContentType) {
    validateOwnership(userId, key);
    validateContentType(expectedContentType);
    StoredObject object = fileStorage.head(key);
    validateContentType(object.contentType());
    validateSize(object.size());
    if (!expectedContentType.equals(object.contentType())) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }
  }

  public void validatePublicObject(String key, String expectedContentType) {
    validatePublicKey(key);
    validateContentType(expectedContentType);
    StoredObject object = fileStorage.head(key);
    validateContentType(object.contentType());
    validateSize(object.size());
    if (!expectedContentType.equals(object.contentType())) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }
  }

  public ConfirmResponse confirm(Long userId, String key) {
    validateOwnership(userId, key);
    StoredObject object = fileStorage.head(key);
    validateContentType(object.contentType());
    validateSize(object.size());
    return new ConfirmResponse(key, fileStorage.buildUrl(key));
  }

  public PublicUploadResponse uploadPublic(String purpose, String contentType, byte[] content) {
    validateContentType(contentType);
    validateContent(content);
    validateSize(content.length);
    String key = buildPublicKey(purpose, contentType);
    fileStorage.upload(key, contentType, content);
    return new PublicUploadResponse(key, fileStorage.buildUrl(key));
  }

  private void validateContentType(String contentType) {
    if (contentType == null || !properties.allowedContentTypes().contains(contentType)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }
  }

  private void validateSize(long size) {
    if (size <= 0) {
      throw new BusinessException(ErrorCode.EMPTY_FILE);
    }
    if (size > properties.maxFileSize()) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }
  }

  private void validateContent(byte[] content) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.EMPTY_FILE);
    }
  }

  private void validateOwnership(Long userId, String key) {
    if (key == null || !isOwnedBy(userId, key)) {
      throw new BusinessException(ErrorCode.FORBIDDEN_FILE_ACCESS);
    }
  }

  private boolean isOwnedBy(Long userId, String key) {
    String[] segments = key.split("/");
    return segments.length == 4
        && segments[0].equals("images")
        && segments[2].equals(String.valueOf(userId));
  }

  private void validatePublicKey(String key) {
    if (key == null || !key.startsWith("public/")) {
      throw new BusinessException(ErrorCode.FORBIDDEN_FILE_ACCESS);
    }
  }

  private String buildKey(Long userId, String purpose, String contentType) {
    String normalizedPurpose = normalizePurpose(purpose);
    String extension = EXTENSIONS.getOrDefault(contentType, "bin");
    return "images/%s/%d/%s.%s".formatted(normalizedPurpose, userId, UUID.randomUUID(), extension);
  }

  private String buildPublicKey(String purpose, String contentType) {
    String normalizedPurpose = normalizePurpose(purpose);
    String extension = EXTENSIONS.getOrDefault(contentType, "bin");
    return "public/%s/%s.%s".formatted(normalizedPurpose, UUID.randomUUID(), extension);
  }

  private String normalizePurpose(String purpose) {
    if (purpose == null || purpose.isBlank()) {
      return "etc";
    }
    if (!PURPOSE_PATTERN.matcher(purpose).matches()) {
      throw new BusinessException(ErrorCode.INVALID_UPLOAD_PURPOSE);
    }
    return purpose;
  }
}

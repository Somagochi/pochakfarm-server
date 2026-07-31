package com.somagochi.pochakfarm.storage.infrastructure;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.domain.PresignedUpload;
import com.somagochi.pochakfarm.storage.domain.StoredObject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** 테스트용 인메모리 FileStorage 페이크. */
public class InMemoryFileStorage implements FileStorage {

  private final Map<String, StoredObject> objects = new HashMap<>();

  /** 업로드가 끝난 것처럼 오브젝트를 미리 채워 넣는다. */
  public void put(String key, long size, String contentType) {
    objects.put(key, new StoredObject(key, size, contentType));
  }

  @Override
  public PresignedUpload presignPut(String key, String contentType, Duration ttl) {
    return new PresignedUpload("https://upload.test/" + key, Instant.EPOCH.plus(ttl));
  }

  @Override
  public PresignedUpload presignGet(String key, Duration ttl) {
    return new PresignedUpload("https://download.test/" + key, Instant.EPOCH.plus(ttl));
  }

  @Override
  public void upload(String key, String contentType, byte[] content) {
    objects.put(key, new StoredObject(key, content.length, contentType));
  }

  @Override
  public StoredObject head(String key) {
    StoredObject object = objects.get(key);
    if (object == null) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
    return object;
  }

  @Override
  public String buildUrl(String key) {
    return "https://cdn.test/" + key;
  }
}

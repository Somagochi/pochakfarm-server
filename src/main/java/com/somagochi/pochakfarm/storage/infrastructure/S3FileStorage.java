package com.somagochi.pochakfarm.storage.infrastructure;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.S3Properties;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.domain.PresignedUpload;
import com.somagochi.pochakfarm.storage.domain.StoredObject;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class S3FileStorage implements FileStorage {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Properties properties;

  public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.properties = properties;
  }

  @Override
  public PresignedUpload presignPut(String key, String contentType, Duration ttl) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(key)
            .contentType(contentType)
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .putObjectRequest(objectRequest)
            .build();
    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
    return new PresignedUpload(presigned.url().toString(), presigned.expiration());
  }

  @Override
  public PresignedUpload presignGet(String key, Duration ttl) {
    GetObjectRequest objectRequest =
        GetObjectRequest.builder().bucket(properties.bucket()).key(key).build();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(objectRequest)
            .build();
    PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
    return new PresignedUpload(presigned.url().toString(), presigned.expiration());
  }

  @Override
  public void upload(String key, String contentType, byte[] content) {
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(key)
            .contentType(contentType)
            .build();
    s3Client.putObject(objectRequest, RequestBody.fromBytes(content));
  }

  @Override
  public StoredObject head(String key) {
    try {
      HeadObjectResponse response =
          s3Client.headObject(builder -> builder.bucket(properties.bucket()).key(key));
      return new StoredObject(key, response.contentLength(), response.contentType());
    } catch (NoSuchKeyException exception) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
  }

  @Override
  public void copy(String sourceKey, String targetKey) {
    try {
      s3Client.copyObject(
          builder ->
              builder
                  .sourceBucket(properties.bucket())
                  .sourceKey(sourceKey)
                  .destinationBucket(properties.bucket())
                  .destinationKey(targetKey));
    } catch (NoSuchKeyException exception) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
  }

  @Override
  public String buildUrl(String key) {
    return s3Client
        .utilities()
        .getUrl(builder -> builder.bucket(properties.bucket()).key(key))
        .toString();
  }
}

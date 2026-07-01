package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

  @Bean
  AwsCredentialsProvider awsCredentialsProvider(S3Properties properties) {
    if (hasAccessKey(properties)) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }
    return DefaultCredentialsProvider.builder().build();
  }

  @Bean
  S3Client s3Client(S3Properties properties, AwsCredentialsProvider awsCredentialsProvider) {
    return S3Client.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }

  @Bean
  S3Presigner s3Presigner(S3Properties properties, AwsCredentialsProvider awsCredentialsProvider) {
    return S3Presigner.builder()
        .region(Region.of(properties.region()))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }

  private boolean hasAccessKey(S3Properties properties) {
    return properties.accessKey() != null && !properties.accessKey().isBlank();
  }
}

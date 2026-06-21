package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_users_provider_provider_id",
            columnNames = {"provider", "provider_id"}))
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Embedded private SocialAccount socialAccount;

  private String email;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;

  private User(SocialAccount socialAccount, String email) {
    this.socialAccount = socialAccount;
    this.email = email;
  }

  public static User register(SocialProvider provider, String providerId, String email) {
    return new User(new SocialAccount(provider, providerId), email);
  }
}

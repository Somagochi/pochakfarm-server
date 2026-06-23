package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "users",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_users_provider_provider_id",
            columnNames = {"provider", "provider_id"}))
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Embedded private SocialAccount socialAccount;

  private String email;

  private User(SocialAccount socialAccount, String email) {
    this.socialAccount = socialAccount;
    this.email = email;
  }

  public static User register(SocialProvider provider, String providerId, String email) {
    return new User(new SocialAccount(provider, providerId), email);
  }
}

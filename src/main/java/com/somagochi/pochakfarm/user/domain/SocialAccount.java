package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, updatable = false)
  private SocialProvider provider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  public SocialAccount(SocialProvider provider, String providerId) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.providerId = Objects.requireNonNull(providerId, "providerId must not be null");
  }

  SocialAccount anonymized(String token) {
    return new SocialAccount(provider, "deleted-" + token + "-" + providerId);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SocialAccount that)) {
      return false;
    }
    return provider == that.provider && providerId.equals(that.providerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, providerId);
  }
}

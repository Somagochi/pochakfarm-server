package com.somagochi.pochakfarm.user.domain;

import com.somagochi.pochakfarm.common.entity.BaseEntity;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_users_provider_email",
          columnNames = {"provider", "email"}),
      @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
    })
@SQLRestriction("deleted_at is null")
public class User extends BaseEntity {

  private static final int MAX_NICKNAME_LENGTH = 6;
  private static final int INITIAL_LEVEL = 1;
  private static final long INITIAL_COINS = 1000L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Embedded private SocialAccount socialAccount;

  private String email;

  private String nickname;

  private int level;

  private long experience;

  @Embedded private Coin coins;

  private User(SocialAccount socialAccount, String email) {
    this.socialAccount = socialAccount;
    this.email = email;
    this.level = INITIAL_LEVEL;
    this.coins = Coin.of(INITIAL_COINS);
  }

  public static User register(SocialProvider provider, String providerId, String email) {
    return new User(new SocialAccount(provider, providerId), email);
  }

  public void changeNickname(String nickname) {
    if (nickname == null) {
      throw new BusinessException(ErrorCode.INVALID_NICKNAME);
    }
    String trimmed = nickname.trim();
    if (trimmed.length() > MAX_NICKNAME_LENGTH || trimmed.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_NICKNAME);
    }
    this.nickname = trimmed;
  }

  public long getCoins() {
    return coins.value();
  }

  public void spendCoins(long amount) {
    this.coins = coins.spend(amount);
  }

  public void addCoins(long amount) {
    this.coins = coins.add(amount);
  }

  public LevelReward gainExperience(long experienceReward, LevelRewardPolicy levelRewardPolicy) {
    LevelReward reward = levelRewardPolicy.calculate(level, experience, experienceReward);
    this.level = reward.levelAfter();
    this.experience = reward.experienceAfter();
    if (reward.coinReward() > 0) {
      this.coins = coins.add(reward.coinReward());
    }
    return reward;
  }

  public void addExperience(long amount) {
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    this.experience += amount;
  }

  public void withdraw() {
    if (isDeleted()) {
      return;
    }
    String token = UUID.randomUUID().toString();
    delete(Instant.now());
    this.socialAccount = socialAccount.anonymized(token);
    this.nickname = null;
    if (email != null) {
      this.email = "deleted-" + token + "-" + email;
    }
  }
}

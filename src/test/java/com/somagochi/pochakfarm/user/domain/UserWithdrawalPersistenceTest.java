package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.common.social.SocialProvider;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserWithdrawalPersistenceTest {

  @Autowired private EntityManager entityManager;

  @Test
  void persistsWithdrawalReasonAsStringWhileSoftDeletingUser() {
    User user =
        User.register(
            SocialProvider.KAKAO,
            "provider-id",
            "user@example.com",
            "u" + UUID.randomUUID().toString().substring(0, 5));
    entityManager.persist(user);
    entityManager.flush();

    user.withdraw(WithdrawalReason.NEW_ACCOUNT);
    entityManager.flush();

    Object reason =
        entityManager
            .createNativeQuery("SELECT withdrawal_reason FROM users WHERE id = :id")
            .setParameter("id", user.getId())
            .getSingleResult();

    assertEquals(WithdrawalReason.NEW_ACCOUNT.name(), reason.toString());
  }
}

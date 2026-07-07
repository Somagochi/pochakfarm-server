package com.somagochi.pochakfarm.common.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AfterCommitExecutorTest {

  @Test
  void runsImmediatelyWhenNoTransactionActive() {
    AtomicBoolean ran = new AtomicBoolean(false);

    AfterCommitExecutor.execute(() -> ran.set(true));

    assertTrue(ran.get());
  }

  @Test
  void defersUntilCommitWhenTransactionActive() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      AtomicBoolean ran = new AtomicBoolean(false);

      AfterCommitExecutor.execute(() -> ran.set(true));
      assertFalse(ran.get());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      assertTrue(ran.get());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}

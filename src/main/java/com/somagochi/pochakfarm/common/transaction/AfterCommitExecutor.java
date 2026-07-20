package com.somagochi.pochakfarm.common.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 현재 트랜잭션이 커밋된 이후 실행할 동작을 등록한다. 트랜잭션이 없으면 즉시 실행한다. */
public final class AfterCommitExecutor {

  private AfterCommitExecutor() {}

  public static void execute(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }
}

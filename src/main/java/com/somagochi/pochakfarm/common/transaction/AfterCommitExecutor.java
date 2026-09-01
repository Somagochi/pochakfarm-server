package com.somagochi.pochakfarm.common.transaction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;
import org.springframework.core.task.TaskExecutor;
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

  public static void executeAsyncAfterCommit(TaskExecutor executor, Runnable action) {
    executeTimedAsyncAfterCommit(executor, ignored -> action.run());
  }

  public static void executeTimedAsyncAfterCommit(TaskExecutor executor, LongConsumer action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      executeTimed(executor, action);
      return;
    }

    CountDownLatch transactionCompleted = new CountDownLatch(1);
    AtomicBoolean committed = new AtomicBoolean(false);
    executeTimed(
        executor,
        queueDurationNanos -> {
          if (await(transactionCompleted) && committed.get()) {
            action.accept(queueDurationNanos);
          }
        });
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            committed.set(status == STATUS_COMMITTED);
            transactionCompleted.countDown();
          }
        });
  }

  private static void executeTimed(TaskExecutor executor, LongConsumer action) {
    long submittedAtNanos = System.nanoTime();
    executor.execute(() -> action.accept(Math.max(0L, System.nanoTime() - submittedAtNanos)));
  }

  private static boolean await(CountDownLatch transactionCompleted) {
    try {
      transactionCompleted.await();
      return true;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}

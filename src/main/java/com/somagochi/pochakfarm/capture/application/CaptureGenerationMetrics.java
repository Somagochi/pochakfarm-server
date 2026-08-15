package com.somagochi.pochakfarm.capture.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CaptureGenerationMetrics {

  static final String METRIC_NAME = "capture.generation.duration";

  private final MeterRegistry meterRegistry;

  public CaptureGenerationMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public <T> T recordCharacterizer(Supplier<T> action) {
    long startedAtNanos = System.nanoTime();
    try {
      T result = action.get();
      record("characterizer", "success", System.nanoTime() - startedAtNanos);
      return result;
    } catch (RuntimeException exception) {
      record("characterizer", "failure", System.nanoTime() - startedAtNanos);
      throw exception;
    }
  }

  public void recordQueue(long durationNanos, String outcome) {
    record("queue", outcome, durationNanos);
  }

  public void recordTotal(long durationNanos, String outcome) {
    record("total", outcome, durationNanos);
  }

  private void record(String stage, String outcome, long durationNanos) {
    Timer.builder(METRIC_NAME)
        .description("App capture generation duration by stage and outcome")
        .tag("stage", stage)
        .tag("outcome", outcome)
        .publishPercentileHistogram()
        .register(meterRegistry)
        .record(Math.max(0L, durationNanos), TimeUnit.NANOSECONDS);
  }
}

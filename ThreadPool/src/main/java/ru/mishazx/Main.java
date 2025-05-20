package ru.mishazx;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final AtomicInteger completedTasks = new AtomicInteger();
    private static final AtomicInteger rejectedTasks  = new AtomicInteger();

    public static void main(String[] args) {

        MyCustomThreadPool pool = new MyCustomThreadPool(
                2,           // corePoolSize
                4,                       // maxPoolSize
                5,                       // keepAliveTime
                TimeUnit.SECONDS,        // timeUnit
                5,                       // queueSize
                1                        // minSpareThreads
        );

        System.out.println("▶  STARTING THREAD-POOL DEMO\n");

        runScenario(pool, "Normal load",          10, 1_000);
        runScenario(pool, "High load / rejects",  20,   500);
        runScenario(pool, "Burst traffic",        30,   200);
        runScenario(pool, "Long-running tasks",    5, 5_000);

        System.out.println("\n▶  SHUTTING DOWN POOL …");
        pool.shutdown();

        // итоговая статистика
        System.out.println("\n===== FINAL STATS =====");
        System.out.println("Completed tasks: " + completedTasks.get());
        System.out.println("Rejected  tasks: " + rejectedTasks .get());
    }

    /* ---------------- private helpers ---------------- */

    private static void runScenario(MyCustomThreadPool pool,
                                    String title,
                                    int taskCount,
                                    int taskDurationMs) {

        System.out.printf("\n--- Scenario: %s ---\n", title);
        submitTasks(pool, taskCount, taskDurationMs);
        sleepSeconds(15);
    }

    private static void submitTasks(MyCustomThreadPool pool,
                                    int count,
                                    int sleepTimeMs) {

        for (int i = 0; i < count; i++) {
            final int taskId = i;

            try {
                pool.execute(() -> {
                    String threadName = Thread.currentThread().getName();
                    System.out.printf("[Task %02d] started by %s%n", taskId, threadName);
                    try {
                        Thread.sleep(sleepTimeMs);
                        completedTasks.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.printf("[Task %02d] finished by %s%n", taskId, threadName);
                });

            } catch (Exception e) {
                rejectedTasks.incrementAndGet();
                System.out.printf("[Task %02d] REJECTED%n", taskId);
            }
        }
    }

    private static void sleepSeconds(int seconds) {
        System.out.printf("… waiting %d s …%n", seconds);
        try {
            Thread.sleep(seconds * 1_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

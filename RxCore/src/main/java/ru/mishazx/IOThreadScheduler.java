package ru.mishazx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IOThreadScheduler implements Scheduler {
    private final ExecutorService executor;

    public IOThreadScheduler() {
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
} 

package ru.mishazx;

import java.util.concurrent.BlockingQueue;

public class Worker implements Runnable {
    private final BlockingQueue<Runnable> queue;
    private final MyCustomThreadPool pool;
    private volatile boolean running = true;

    public Worker(BlockingQueue<Runnable> queue, MyCustomThreadPool pool) {
        this.queue = queue;
        this.pool = pool;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try {
            while (running) {
                Runnable task = queue.poll(pool.keepAliveTime, pool.timeUnit);
                if (task != null) {
                    pool.onTaskStart();
                    try {
                        task.run();
                    } finally {
                        pool.onTaskFinish();
                    }
                } else if (pool.poolSize.get() > pool.corePoolSize) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.onWorkerExit(this, queue);
        }
    }
}

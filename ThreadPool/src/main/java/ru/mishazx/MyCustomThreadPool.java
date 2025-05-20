package ru.mishazx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MyCustomThreadPool {
    final int corePoolSize;
    private final int maxPoolSize;
    final long keepAliveTime;
    final TimeUnit timeUnit;
    private final int queueCapacity;

    private final List<Worker> workers = new ArrayList<>();
    private final List<BlockingQueue<Runnable>> taskQueues = new ArrayList<>();
    private final CustomThreadFactory threadFactory = new CustomThreadFactory();
    private final RejectedExecutionHandler rejectionHandler = new CustomRejectionHandler();

    final AtomicInteger poolSize = new AtomicInteger(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger queueIndex = new AtomicInteger(0);

    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean isShutdown = false;

    public MyCustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime,
                              TimeUnit timeUnit, int queueCapacity, int minSpareThreads) {
        if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize
                || keepAliveTime < 0 || queueCapacity <= 0 || minSpareThreads < 0) {
            throw new IllegalArgumentException("Invalid thread pool parameters");
        }

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueCapacity = queueCapacity;

        initializeCoreWorkers();
    }

    private void initializeCoreWorkers() {
        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
        System.out.println("Thread pool initialized with " + corePoolSize + " core threads");
    }

    private void addWorker() {
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        Worker worker = new Worker(taskQueue, this);
        Thread thread = threadFactory.newThread(worker);

        taskQueues.add(taskQueue);
        workers.add(worker);
        poolSize.incrementAndGet();

        thread.start();
    }

    public void execute(Runnable task) {
        if (task == null) throw new NullPointerException("Task cannot be null");

        if (isShutdown) {
            rejectionHandler.rejectedExecution(task, null);
            return;
        }

        lock.lock();
        try {
            if (activeTasks.get() >= poolSize.get() && poolSize.get() < maxPoolSize) {
                addWorker();
                System.out.println("Created new worker. Current pool size: " + poolSize.get());
            }

            BlockingQueue<Runnable> queue = selectQueue();
            if (!queue.offer(task)) {
                rejectionHandler.rejectedExecution(task, null);
            } else {
                System.out.println("Task submitted to queue index: " + taskQueues.indexOf(queue));
            }
        } finally {
            lock.unlock();
        }
    }

    private BlockingQueue<Runnable> selectQueue() {
        int index = queueIndex.getAndUpdate(i -> (i + 1) % taskQueues.size());
        return taskQueues.get(index);
    }

    public void shutdown() {
        lock.lock();
        try {
            isShutdown = true;
            for (Worker worker : workers) {
                worker.stop();
            }
            System.out.println("Thread pool shutdown initiated");
        } finally {
            lock.unlock();
        }
    }

    protected void onWorkerExit(Worker worker, BlockingQueue<Runnable> queue) {
        poolSize.decrementAndGet();
        lock.lock();
        try {
            workers.remove(worker);
            taskQueues.remove(queue);
        } finally {
            lock.unlock();
        }
        System.out.println("Worker terminated. Current pool size: " + poolSize.get());
    }

    protected void onTaskStart() {
        activeTasks.incrementAndGet();
    }

    protected void onTaskFinish() {
        activeTasks.decrementAndGet();
    }
}

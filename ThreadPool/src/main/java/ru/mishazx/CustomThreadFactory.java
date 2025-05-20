package ru.mishazx;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Worker-" + threadNumber.getAndIncrement());
        System.out.println("Created thread: " + t.getName());
        return t;
    }
}

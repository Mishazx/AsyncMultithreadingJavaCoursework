package ru.mishazx;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SchedulersTest {

    @Test
    void testIOSchedulerExecutesTask() throws InterruptedException {
        // Создаем CountDownLatch для ожидания завершения задачи
        CountDownLatch latch = new CountDownLatch(1);
        
        // Переменная для хранения имени потока
        AtomicReference<String> threadName = new AtomicReference<>();
        
        // Получаем IO планировщик
        Scheduler ioScheduler = Schedulers.io();
        
        // Выполняем задачу
        ioScheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        // Ждем завершения задачи
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Проверяем, что задача выполнена в отдельном потоке
        assertNotNull(threadName.get());
        assertNotEquals(Thread.currentThread().getName(), threadName.get());
    }
    
    @Test
    void testComputationSchedulerExecutesTask() throws InterruptedException {
        // Создаем CountDownLatch для ожидания завершения задачи
        CountDownLatch latch = new CountDownLatch(1);
        
        // Переменная для хранения имени потока
        AtomicReference<String> threadName = new AtomicReference<>();
        
        // Получаем Computation планировщик
        Scheduler computationScheduler = Schedulers.computation();
        
        // Выполняем задачу
        computationScheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        // Ждем завершения задачи
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Проверяем, что задача выполнена в отдельном потоке
        assertNotNull(threadName.get());
        assertNotEquals(Thread.currentThread().getName(), threadName.get());
    }
    
    @Test
    void testSingleThreadSchedulerExecutesTask() throws InterruptedException {
        // Создаем CountDownLatch для ожидания завершения задачи
        CountDownLatch latch = new CountDownLatch(1);
        
        // Переменная для хранения имени потока
        AtomicReference<String> threadName = new AtomicReference<>();
        
        // Получаем Single планировщик
        Scheduler singleScheduler = Schedulers.single();
        
        // Выполняем задачу
        singleScheduler.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        
        // Ждем завершения задачи
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Проверяем, что задача выполнена в отдельном потоке
        assertNotNull(threadName.get());
        assertNotEquals(Thread.currentThread().getName(), threadName.get());
    }
    
    @Test
    void testSingleThreadSchedulerUsesTheSameThread() throws InterruptedException {
        // Создаем CountDownLatch для ожидания завершения задач
        CountDownLatch latch = new CountDownLatch(2);
        
        // Множество для хранения имен потоков
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        
        // Получаем Single планировщик
        Scheduler singleScheduler = Schedulers.single();
        
        // Выполняем первую задачу
        singleScheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            latch.countDown();
        });
        
        // Выполняем вторую задачу
        singleScheduler.execute(() -> {
            threadNames.add(Thread.currentThread().getName());
            latch.countDown();
        });
        
        // Ждем завершения задач
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Проверяем, что обе задачи выполнены в одном потоке
        assertEquals(1, threadNames.size());
    }
    
    @Test
    void testComputationSchedulerUsesCores() throws InterruptedException {
        // Получаем количество доступных процессоров
        int processors = Runtime.getRuntime().availableProcessors();
        
        // Если есть только один процессор, тест пропускается
        if (processors <= 1) {
            return;
        }
        
        // Создаем CountDownLatch для ожидания завершения задач
        CountDownLatch latch = new CountDownLatch(processors);
        
        // Множество для хранения имен потоков
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        
        // Получаем Computation планировщик
        Scheduler computationScheduler = Schedulers.computation();
        
        // Выполняем задачи, блокирующие потоки
        for (int i = 0; i < processors; i++) {
            computationScheduler.execute(() -> {
                try {
                    // Добавляем имя потока
                    threadNames.add(Thread.currentThread().getName());
                    
                    // Блокируем поток на короткое время
                    Thread.sleep(100);
                    
                    // Сигнализируем о завершении
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Ждем завершения задач
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Проверяем, что задачи выполнены в разных потоках
        assertTrue(threadNames.size() > 1);
    }
    
    @Test
    void testIOSchedulerCanHandleManyTasks() throws InterruptedException {
        // Количество задач
        int taskCount = 100;
        
        // Создаем CountDownLatch для ожидания завершения задач
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        // Множество для хранения имен потоков
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        
        // Получаем IO планировщик
        Scheduler ioScheduler = Schedulers.io();
        
        // Выполняем задачи
        for (int i = 0; i < taskCount; i++) {
            ioScheduler.execute(() -> {
                try {
                    // Добавляем имя потока
                    threadNames.add(Thread.currentThread().getName());
                    
                    // Небольшая задержка
                    Thread.sleep(10);
                    
                    // Сигнализируем о завершении
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Ждем завершения задач
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Проверяем, что для выполнения задач было создано несколько потоков
        assertTrue(threadNames.size() > 1);
    }
} 

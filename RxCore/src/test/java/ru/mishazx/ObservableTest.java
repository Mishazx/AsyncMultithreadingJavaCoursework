package ru.mishazx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ObservableTest {

    private List<Integer> receivedItems;
    private Throwable receivedError;
    private boolean completed;
    private Observer<Integer> testObserver;

    @BeforeEach
    void setUp() {
        receivedItems = new ArrayList<>();
        receivedError = null;
        completed = false;
        
        testObserver = new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                receivedItems.add(item);
            }

            @Override
            public void onError(Throwable t) {
                receivedError = t;
            }

            @Override
            public void onComplete() {
                completed = true;
            }
        };
    }

    @Test
    void testBasicObservableEmitsItemsAndCompletes() {
        // Создаем Observable, который эмитит числа 1, 2, 3
        Observable<Integer> observable = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        // Подписываемся на Observable
        observable.subscribe(testObserver);

        // Проверяем, что мы получили ожидаемые элементы
        assertEquals(List.of(1, 2, 3), receivedItems);
        // Проверяем, что Observable завершился
        assertTrue(completed);
        // Проверяем, что не было ошибок
        assertNull(receivedError);
    }

    @Test
    void testMapOperator() {
        // Создаем Observable и применяем map для преобразования чисел в строки
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        List<String> mappedItems = new ArrayList<>();
        Observable<String> mapped = source.map(i -> "Number: " + i);
        
        mapped.subscribe(
            mappedItems::add,
            e -> fail("Should not throw error"),
            () -> completed = true
        );

        // Проверяем результаты преобразования
        assertEquals(List.of("Number: 1", "Number: 2"), mappedItems);
        assertTrue(completed);
    }

    @Test
    void testFilterOperator() {
        // Создаем Observable и применяем filter для фильтрации четных чисел
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onNext(4);
            observer.onComplete();
        });

        Observable<Integer> filtered = source.filter(i -> i % 2 == 0);
        
        filtered.subscribe(testObserver);

        // Проверяем, что остались только четные числа
        assertEquals(List.of(2, 4), receivedItems);
        assertTrue(completed);
    }

    @Test
    void testFlatMapOperator() {
        // Создаем Observable и применяем flatMap
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onComplete();
        });

        Observable<String> flatMapped = source.flatMap(i -> 
            Observable.create(innerObserver -> {
                innerObserver.onNext("Item " + i + ": A");
                innerObserver.onNext("Item " + i + ": B");
                innerObserver.onComplete();
            })
        );

        List<String> results = new ArrayList<>();
        flatMapped.subscribe(
            results::add,
            e -> fail("Should not throw error"),
            () -> completed = true
        );

        // Проверяем результаты flatMap
        assertEquals(4, results.size());
        assertTrue(results.contains("Item 1: A"));
        assertTrue(results.contains("Item 1: B"));
        assertTrue(results.contains("Item 2: A"));
        assertTrue(results.contains("Item 2: B"));
        assertTrue(completed);
    }

    @Test
    void testErrorHandling() {
        // Создаем Observable, который выбрасывает исключение
        Observable<Integer> errorObservable = Observable.create(observer -> {
            observer.onNext(1);
            throw new RuntimeException("Test error");
        });

        errorObservable.subscribe(testObserver);

        // Проверяем, что мы получили элемент перед ошибкой
        assertEquals(List.of(1), receivedItems);
        // Проверяем, что мы получили ошибку
        assertNotNull(receivedError);
        assertEquals("Test error", receivedError.getMessage());
        // Проверяем, что Observable не завершился нормально
        assertFalse(completed);
    }

    @Test
    void testSubscribeOnAndObserveOn() throws InterruptedException {
        // Создаем CountDownLatch для ожидания завершения асинхронных операций
        CountDownLatch latch = new CountDownLatch(1);
        
        // Сохраняем имена потоков для проверки
        AtomicReference<String> emissionThread = new AtomicReference<>();
        AtomicReference<String> observationThread = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(observer -> {
            emissionThread.set(Thread.currentThread().getName());
            observer.onNext(1);
            observer.onComplete();
        });

        observable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe(
                i -> {
                    observationThread.set(Thread.currentThread().getName());
                    receivedItems.add(i);
                },
                e -> fail("Should not throw error"),
                () -> {
                    completed = true;
                    latch.countDown();
                }
            );

        // Ждем завершения асинхронных операций
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Проверяем, что мы получили элемент
        assertEquals(List.of(1), receivedItems);
        
        // Проверяем, что потоки выполнения отличаются от текущего потока
        assertNotEquals(Thread.currentThread().getName(), emissionThread.get());
        assertNotEquals(Thread.currentThread().getName(), observationThread.get());
        
        // Проверяем, что потоки эмиссии и наблюдения отличаются
        // Из-за использования разных планировщиков
        assertNotEquals(emissionThread.get(), observationThread.get());
    }

    @Test
    void testDisposable() {
        // Создаем счетчик для отслеживания количества эмитированных элементов
        AtomicInteger emissionCount = new AtomicInteger(0);
        
        // Создаем Observable с задержкой между эмиссиями
        Observable<Integer> observable = Observable.create(observer -> {
            new Thread(() -> {
                try {
                    observer.onNext(1);
                    emissionCount.incrementAndGet();
                    Thread.sleep(100);
                    
                    // Этот элемент не должен быть получен после отмены подписки
                    observer.onNext(2);
                    emissionCount.incrementAndGet();
                    Thread.sleep(100);
                    
                    observer.onComplete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });

        // Подписываемся и сохраняем Disposable
        Disposable disposable = observable.subscribe(testObserver);
        
        try {
            // Даем время для эмиссии первого элемента
            Thread.sleep(50);
            
            // Отменяем подписку
            disposable.dispose();
            
            // Проверяем, что подписка отменена
            assertTrue(disposable.isDisposed());
            
            // Даем время для попытки эмиссии второго элемента
            Thread.sleep(200);
            
            // Проверяем, что мы получили только первый элемент
            assertEquals(List.of(1), receivedItems);
            
            // Проверяем, что было эмитировано два элемента, но получен только один
            assertEquals(2, emissionCount.get());
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
    }
} 

# Отчет о реализации библиотеки RxCore

## Введение

Данный отчет описывает реализацию библиотеки RxCore, которая представляет собой упрощенную версию библиотеки RxJava. Библиотека построена на основе концепций реактивного программирования и предоставляет инструменты для асинхронной обработки потоков данных.

## Архитектура системы

Архитектура библиотеки RxCore основана на паттерне "Наблюдатель" (Observer pattern) и включает следующие ключевые компоненты:

### 1. Базовые компоненты

#### 1.1. Интерфейс Observer

Интерфейс `Observer<T>` определяет контракт для объектов, которые хотят получать уведомления от `Observable`. Он содержит три метода:

```java
public interface Observer<T> {
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}
```

- `onNext(T item)` - вызывается для передачи нового элемента наблюдателю
- `onError(Throwable t)` - вызывается при возникновении ошибки
- `onComplete()` - вызывается при успешном завершении потока данных

#### 1.2. Класс Observable

Класс `Observable<T>` представляет источник данных, на который можно подписаться. Основные методы:

```java
public class Observable<T> {
    private final Consumer<Observer<T>> source;

    private Observable(Consumer<Observer<T>> source) {
        this.source = source;
    }

    public static <T> Observable<T> create(Consumer<Observer<T>> source) {
        return new Observable<>(source);
    }

    public Disposable subscribe(Observer<T> observer) {
        // Реализация подписки
    }
}
```

- Конструктор принимает `Consumer<Observer<T>>`, который определяет логику эмиссии элементов
- Статический метод `create()` позволяет создавать новые экземпляры `Observable`
- Метод `subscribe()` позволяет подписаться на получение данных

#### 1.3. Интерфейс Disposable

Интерфейс `Disposable` предоставляет механизм для отмены подписки:

```java
public interface Disposable {
    void dispose();
    boolean isDisposed();
}
```

- `dispose()` - отменяет подписку
- `isDisposed()` - проверяет, отменена ли подписка

### 2. Операторы преобразования данных

#### 2.1. Оператор map

Оператор `map` преобразует элементы потока с помощью заданной функции:

```java
public <R> Observable<R> map(Function<T, R> mapper) {
    return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    observer.onNext(mapper.apply(item));
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
    ));
}
```

#### 2.2. Оператор filter

Оператор `filter` отфильтровывает элементы потока на основе заданного предиката:

```java
public Observable<T> filter(Predicate<T> predicate) {
    return new Observable<>(observer -> subscribe(
            item -> {
                try {
                    if (predicate.test(item)) {
                        observer.onNext(item);
                    }
                } catch (Exception e) {
                    observer.onError(e);
                }
            },
            observer::onError,
            observer::onComplete
    ));
}
```

#### 2.3. Оператор flatMap

Оператор `flatMap` преобразует каждый элемент потока в новый `Observable` и объединяет результаты:

```java
public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
    return new Observable<>(observer -> {
        AtomicBoolean disposed = new AtomicBoolean(false);
        subscribe(
                item -> {
                    if (!disposed.get()) {
                        try {
                            Observable<R> innerObservable = mapper.apply(item);
                            innerObservable.subscribe(
                                    observer::onNext,
                                    observer::onError,
                                    () -> {} // Don't complete when inner completes
                            );
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }
                },
                observer::onError,
                observer::onComplete
        );
    });
}
```

### 3. Управление потоками выполнения

#### 3.1. Интерфейс Scheduler

Интерфейс `Scheduler` определяет способ выполнения задач:

```java
public interface Scheduler {
    void execute(Runnable task);
}
```

#### 3.2. Реализации Scheduler

В библиотеке реализованы три типа планировщиков:

1. **IOThreadScheduler** - использует `CachedThreadPool` для операций ввода-вывода:

```java
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
```

2. **ComputationScheduler** - использует `FixedThreadPool` с количеством потоков, равным количеству доступных процессоров:

```java
public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor;

    public ComputationScheduler() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
```

3. **SingleThreadScheduler** - использует один поток для последовательного выполнения задач:

```java
public class SingleThreadScheduler implements Scheduler {
    private final ExecutorService executor;

    public SingleThreadScheduler() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
```

#### 3.3. Класс Schedulers

Утилитный класс `Schedulers` предоставляет доступ к различным типам планировщиков:

```java
public class Schedulers {
    private static final Scheduler IO = new IOThreadScheduler();
    private static final Scheduler COMPUTATION = new ComputationScheduler();
    private static final Scheduler SINGLE = new SingleThreadScheduler();

    public static Scheduler io() {
        return IO;
    }

    public static Scheduler computation() {
        return COMPUTATION;
    }

    public static Scheduler single() {
        return SINGLE;
    }
}
```

#### 3.4. Методы subscribeOn и observeOn

Для управления потоками выполнения в классе `Observable` реализованы методы:

```java
public Observable<T> subscribeOn(Scheduler scheduler) {
    return new Observable<>(observer -> scheduler.execute(() -> subscribe(observer)));
}

public Observable<T> observeOn(Scheduler scheduler) {
    return new Observable<>(observer -> subscribe(
            item -> scheduler.execute(() -> observer.onNext(item)),
            error -> scheduler.execute(() -> observer.onError(error)),
            () -> scheduler.execute(observer::onComplete)
    ));
}
```

- `subscribeOn` определяет, в каком потоке будет выполняться подписка
- `observeOn` определяет, в каком потоке будут обрабатываться элементы

## Принципы работы Schedulers

### IOThreadScheduler

`IOThreadScheduler` основан на `CachedThreadPool`, который создает новые потоки по мере необходимости и повторно использует ранее созданные потоки. Этот планировщик оптимален для операций ввода-вывода, которые часто блокируются (например, сетевые запросы, файловые операции).

Особенности:
- Создает новые потоки при необходимости
- Повторно использует неактивные потоки
- Подходит для большого количества операций ввода-вывода
- Потоки, не используемые в течение 60 секунд, автоматически завершаются

### ComputationScheduler

`ComputationScheduler` основан на `FixedThreadPool` с количеством потоков, равным количеству доступных процессоров. Этот планировщик оптимален для вычислительных задач, не связанных с блокирующими операциями.

Особенности:
- Использует фиксированное количество потоков (равное количеству ядер процессора)
- Предотвращает чрезмерное создание потоков
- Оптимален для CPU-интенсивных задач
- Обеспечивает максимальное использование вычислительных ресурсов

### SingleThreadScheduler

`SingleThreadScheduler` использует один поток для последовательного выполнения задач. Этот планировщик полезен, когда требуется гарантировать, что задачи будут выполняться строго последовательно.

Особенности:
- Использует только один поток
- Гарантирует последовательное выполнение задач
- Полезен для задач, требующих строгого порядка выполнения
- Предотвращает проблемы параллельного доступа к ресурсам

## Процесс тестирования

### 1. Тестирование базовых компонентов

Для тестирования базовых компонентов системы были написаны юнит-тесты, проверяющие:
- Создание Observable и подписку Observer
- Эмиссию элементов и завершение потока
- Обработку ошибок

Пример теста базовой функциональности:

```java
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
```

### 2. Тестирование операторов преобразования

Для тестирования операторов преобразования были написаны тесты, проверяющие:
- Работу оператора map
- Работу оператора filter
- Работу оператора flatMap

Пример теста оператора map:

```java
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
```

### 3. Тестирование многопоточного выполнения

Для тестирования многопоточного выполнения были написаны тесты, проверяющие:
- Работу различных типов Scheduler
- Работу методов subscribeOn и observeOn
- Выполнение задач в разных потоках

Пример теста многопоточного выполнения:

```java
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
    
    // Проверяем, что потоки эмиссии и наблюдения отличаются
    assertNotEquals(emissionThread.get(), observationThread.get());
}
```

### 4. Тестирование обработки ошибок

Для тестирования обработки ошибок были написаны тесты, проверяющие:
- Передачу ошибок в метод onError
- Обработку исключений в операторах

Пример теста обработки ошибок:

```java
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
```

### 5. Тестирование Disposable

Для тестирования механизма отмены подписки были написаны тесты, проверяющие:
- Отмену подписки через Disposable
- Проверку состояния подписки через isDisposed

Пример теста Disposable:

```java
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
```

## Заключение

Разработанная библиотека RxCore представляет собой упрощенную реализацию концепций реактивного программирования. Она включает базовые компоненты для работы с потоками данных, операторы преобразования и механизмы управления потоками выполнения.

Основные достижения проекта:
1. Реализация паттерна "Наблюдатель" для асинхронной обработки данных
2. Создание операторов преобразования данных (map, filter, flatMap)
3. Реализация механизмов управления потоками выполнения через Scheduler
4. Обеспечение возможности отмены подписки через Disposable
5. Разработка комплексных тестов для проверки функциональности

Библиотека может быть использована для асинхронной обработки потоков данных в Java-приложениях, особенно в ситуациях, когда требуется гибкое управление потоками выполнения и преобразование данных. 

# RxCore - Реализация реактивной библиотеки

Этот проект представляет собой упрощенную реализацию библиотеки RxJava, разработанную в рамках учебного задания. Библиотека реализует основные концепции реактивного программирования, включая асинхронную обработку потоков данных, операторы преобразования и управление потоками выполнения.

## Архитектура системы

Библиотека RxCore построена на основе следующих ключевых компонентов:

### 1. Интерфейс Observer

Интерфейс `Observer<T>` представляет наблюдателя, который получает уведомления от `Observable`. Он содержит три метода:
- `onNext(T item)` - вызывается при получении нового элемента
- `onError(Throwable t)` - вызывается при возникновении ошибки
- `onComplete()` - вызывается при завершении потока данных

### 2. Класс Observable

Класс `Observable<T>` представляет поток данных, на который можно подписаться. Основные методы:
- `static <T> Observable<T> create(Consumer<Observer<T>> source)` - создает новый Observable
- `Disposable subscribe(Observer<T> observer)` - подписывает Observer на получение данных
- Операторы преобразования: `map`, `filter`, `flatMap`
- Методы управления потоками: `subscribeOn`, `observeOn`

### 3. Интерфейс Scheduler

Интерфейс `Scheduler` определяет способ выполнения задач в разных потоках. Метод:
- `void execute(Runnable task)` - выполняет задачу в соответствующем потоке

### 4. Реализации Scheduler

- `IOThreadScheduler` - использует `CachedThreadPool` для операций ввода-вывода
- `ComputationScheduler` - использует `FixedThreadPool` с количеством потоков, равным количеству доступных процессоров
- `SingleThreadScheduler` - использует один поток для последовательного выполнения задач

### 5. Класс Schedulers

Утилитный класс для доступа к различным типам планировщиков:
- `io()` - для операций ввода-вывода
- `computation()` - для вычислительных задач
- `single()` - для последовательного выполнения

### 6. Интерфейс Disposable

Интерфейс `Disposable` позволяет отменять подписку:
- `void dispose()` - отменяет подписку
- `boolean isDisposed()` - проверяет, отменена ли подписка

## Принципы работы Schedulers

### IOThreadScheduler

Планировщик для операций ввода-вывода, использует `CachedThreadPool`. Подходит для операций, которые часто блокируются (например, сетевые запросы, файловые операции). Создает новые потоки по мере необходимости и повторно использует ранее созданные потоки.

### ComputationScheduler

Планировщик для вычислительных задач, использует `FixedThreadPool` с количеством потоков, равным количеству доступных процессоров. Оптимален для задач, требующих интенсивных вычислений, так как не создает больше потоков, чем может эффективно использовать процессор.

### SingleThreadScheduler

Планировщик, использующий один поток для последовательного выполнения задач. Полезен, когда требуется гарантировать, что задачи будут выполняться строго последовательно, без параллелизма.

## Примеры использования

### Базовое использование

```java
// Создание Observable
Observable<Integer> numbers = Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onNext(3);
    observer.onComplete();
});

// Подписка на Observable
numbers.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error.getMessage()),
    () -> System.out.println("Completed")
);
```

### Использование операторов преобразования

```java
Observable<Integer> numbers = Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onNext(3);
    observer.onComplete();
});

numbers
    .map(n -> n * 10)                  // Преобразование чисел
    .filter(n -> n > 10)               // Фильтрация чисел
    .subscribe(
        item -> System.out.println("Received: " + item),
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Completed")
    );
```

### Использование Schedulers

```java
Observable<Integer> numbers = Observable.create(observer -> {
    System.out.println("Emitting on thread: " + Thread.currentThread().getName());
    observer.onNext(1);
    observer.onNext(2);
    observer.onComplete();
});

numbers
    .subscribeOn(Schedulers.io())          // Подписка выполняется в IO потоке
    .observeOn(Schedulers.computation())   // Обработка выполняется в Computation потоке
    .subscribe(
        item -> System.out.println("Received: " + item + " on thread: " + Thread.currentThread().getName()),
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Completed on thread: " + Thread.currentThread().getName())
    );
```

### Использование flatMap

```java
Observable<Integer> numbers = Observable.create(observer -> {
    observer.onNext(1);
    observer.onNext(2);
    observer.onComplete();
});

numbers
    .flatMap(n -> Observable.create(innerObserver -> {
        innerObserver.onNext(n * 10);
        innerObserver.onNext(n * 100);
        innerObserver.onComplete();
    }))
    .subscribe(
        item -> System.out.println("Received: " + item),
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Completed")
    );
```

### Отмена подписки

```java
Observable<Integer> infiniteNumbers = Observable.create(observer -> {
    new Thread(() -> {
        int i = 0;
        while (!Thread.currentThread().isInterrupted()) {
            observer.onNext(i++);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }).start();
});

Disposable disposable = infiniteNumbers.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error.getMessage()),
    () -> System.out.println("Completed")
);

// Через некоторое время отменяем подписку
Thread.sleep(500);
disposable.dispose();
System.out.println("Subscription disposed: " + disposable.isDisposed());
```

## Тестирование

Библиотека включает набор юнит-тестов, которые проверяют:
- Базовую функциональность Observable и Observer
- Работу операторов map, filter и flatMap
- Корректность обработки ошибок
- Работу различных типов Scheduler
- Функциональность Disposable для отмены подписки

## Запуск тестов

Для запуска тестов используйте Maven:

```
mvn test
``` 

package ru.mishazx;

import java.util.concurrent.TimeUnit;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("RxCore Demo");
        System.out.println("==========");
        
        // Создаем простой Observable, эмитирующий числа от 1 до 5
        Observable<Integer> numbers = Observable.create(observer -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    System.out.println("Emitting: " + i + " on thread: " + Thread.currentThread().getName());
                    observer.onNext(i);
                    Thread.sleep(100); // Имитация задержки
                }
                observer.onComplete();
            } catch (Exception e) {
                observer.onError(e);
            }
        });
        
        // Демонстрация операторов map и filter
        Disposable disposable = numbers
            .subscribeOn(Schedulers.io()) // Подписка будет выполняться в IO потоке
            .map(n -> {
                System.out.println("Mapping " + n + " on thread: " + Thread.currentThread().getName());
                return "Number: " + n;
            })
            .observeOn(Schedulers.computation()) // Обработка будет происходить в потоке computation
            .filter(s -> {
                System.out.println("Filtering " + s + " on thread: " + Thread.currentThread().getName());
                return s.endsWith("2") || s.endsWith("4");
            })
            .subscribe(
                item -> System.out.println("Received: " + item + " on thread: " + Thread.currentThread().getName()),
                error -> System.err.println("Error: " + error.getMessage()),
                () -> System.out.println("Completed on thread: " + Thread.currentThread().getName())
            );
        
        // Ждем завершения асинхронных операций
        Thread.sleep(1000);
        
        // Демонстрация flatMap
        System.out.println("\nFlatMap Demo");
        System.out.println("===========");
        
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(10);
            observer.onNext(20);
            observer.onComplete();
        });
        
        Disposable flatMapDisposable = source
            .flatMap(n -> Observable.create(innerObserver -> {
                for (int i = 1; i <= 3; i++) {
                    innerObserver.onNext(n * i);
                }
                innerObserver.onComplete();
            }))
            .subscribe(
                item -> System.out.println("FlatMap result: " + item),
                error -> System.err.println("FlatMap error: " + error.getMessage()),
                () -> System.out.println("FlatMap completed")
            );
        
        // Ждем завершения асинхронных операций
        Thread.sleep(500);
        
        System.out.println("\nDisposable Demo");
        System.out.println("==============");
        System.out.println("Disposing subscription...");
        disposable.dispose();
        System.out.println("Is disposed: " + disposable.isDisposed());
    }
}

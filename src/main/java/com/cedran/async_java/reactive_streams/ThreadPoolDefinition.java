package com.cedran.async_java.reactive_streams;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ThreadPoolDefinition {

    public static void main(String[] args) throws InterruptedException {
        Flowable.range(1, 5)
                .subscribeOn(Schedulers.io())
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " 1- " + s))
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " 2- " + s))
                .observeOn(Schedulers.computation())
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " 3- " + s))
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " 4- " + s))
//                .subscribeOn(Schedulers.computation()) doesn't change the next operation scheduler
                .observeOn(Schedulers.io())
                .doOnNext(s -> System.out.println(Thread.currentThread().getName() + " 5- " + s))
                .subscribe(s -> System.out.println(Thread.currentThread().getName() + " 6- " + s));


        Thread.sleep(3000);
    }
}

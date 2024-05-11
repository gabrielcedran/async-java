package com.cedran.async_java.reactive_streams;

import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.TimeUnit;

public class Throttling {

    public static void main(String[] args) {
        Flowable.interval(100, TimeUnit.MILLISECONDS)
//                .sample(1, TimeUnit.SECONDS)
                .buffer(10)
                .blockingSubscribe(System.out::println);


    }
}

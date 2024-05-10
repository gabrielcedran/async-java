package com.cedran.async_java.completable_future;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureExamples {

    public String slowService() {
        System.out.println("Slow service Executed by thread" + Thread.currentThread().getName());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return String.valueOf(System.currentTimeMillis());
    }

    public void composeExample() {
        long totalTimeStart = System.currentTimeMillis();

        var result = CompletableFuture.supplyAsync(() -> {
                    long start = System.currentTimeMillis();
                    var currentResult = slowService();
                    System.out.println("Executing 1 took: " + (System.currentTimeMillis() - start) + "Executed by thread: " + Thread.currentThread().getName());
                    return currentResult;
                })
            .thenComposeAsync(previousResult -> {
                long start = System.currentTimeMillis();
                var currentResult = CompletableFuture.supplyAsync(() -> slowService());
                System.out.println("Executing 2 took: " + (System.currentTimeMillis() - start) + "Executed by thread: " + Thread.currentThread().getName());
                return currentResult;
            }).join(); // this final line blocks - ideally it'd return a completablefuture via the endpoint or whatever the entry point is

        System.out.println("Result: " + result + " - Total time: " + (System.currentTimeMillis() - totalTimeStart));
    }

    public void thenApplyExample() {
        long totalTimeStart = System.currentTimeMillis();

        var result = CompletableFuture.supplyAsync(() -> {
                    long start = System.currentTimeMillis();
                    var currentResult = slowService();
                    System.out.println("Executing 1 took: " + (System.currentTimeMillis() - start) + "Executed by thread: " + Thread.currentThread().getName());
                    return currentResult;
                })
                .thenApplyAsync(previousResult -> {
                    long start = System.currentTimeMillis();
                    var currentResult = slowService();
                    System.out.println("Executing 2 took: " + (System.currentTimeMillis() - start) + "Executed by thread: " + Thread.currentThread().getName());
                    return currentResult;
                }).join(); // this final line blocks - ideally it'd return a completablefuture via the endpoint or whatever the entry point is

        System.out.println("Result: " + result + " - Total time: " + (System.currentTimeMillis() - totalTimeStart));
    }

    public static void main(String[] args) {
        var example = new CompletableFutureExamples();
        // Even though the thread is blocking in the slowService method, these examples show how compose and thenApply differ in their execution (note the time difference being logged)
        // ideally slowService method would also be non-blocking also so that the thenCompose would effectively deliver its full potential (i.e. a non blocking http request via httpClient api)
        example.composeExample();
        example.thenApplyExample();
    }

}

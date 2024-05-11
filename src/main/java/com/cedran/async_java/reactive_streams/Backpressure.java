package com.cedran.async_java.reactive_streams;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Backpressure {

    public static void main(String[] args) {
        Flowable<String> messages = Flowable.fromPublisher(
            subscriber -> {
                subscriber.onSubscribe(new Subscription() {
                    int count = 0;

                    @Override
                    public void request(long l) {
                        for (int i = 0; i < l; i++)
                            subscriber.onNext("Hello" + count++);

                        if (count >= 20) {
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                        subscriber.onComplete();
                    }
                });
            }
        );

        messages.map(m -> m + "?").blockingSubscribe(new Subscriber<>() {
            Subscription subscription;
            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(4);
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    System.out.println("Asking more 3");
                    subscription.request(3);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error :(");
            }

            @Override
            public void onComplete() {
                System.out.println("Done!");
            }
        });
    }
}

package com.cedran.async_java.reactive_streams;

import io.reactivex.rxjava3.core.Flowable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class Examples {

    private static List<Client> clients = List.of(new Client("Bob", 10), new Client("Mary", 30), new Client("Don", 10), new Client("Gabriel", 32));


    public static void main(String[] args) {
//        Flowable.fromArray(clients.toArray(new Client[0]))
//                .filter(c -> c.getAge() == 10)
//                .map(Client::getName)
//                .subscribe(System.out::println);

//        Flowable.fromArray(clients.toArray(new Client[0]))
//                .doOnNext(System.out::println)
//                .any(c -> c.getAge() == 10)
//                .subscribe(System.out::println);

//        Flowable.interval(1, TimeUnit.SECONDS)
//                .map(i -> i + 1)
//                .takeUntil(i -> i == 5)
//                .reduce(0l, (acc, value) -> acc + value)
//                .blockingSubscribe(System.out::println);

//        Flowable.just(1, "a", true)
//                .blockingSubscribe(System.out::println);

        subscriberExample();

    }

    private static void subscriberExample() {


        Flowable<String> messages = Flowable.fromPublisher(subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {

                }

                @Override
                public void cancel() {

                }
            });

            subscriber.onNext("Hello");
            subscriber.onNext("Hello 2");
            subscriber.onNext("Hello 3");
            subscriber.onComplete();
//            subscriber.onError(new RuntimeException("Oops"));
        });

        // onNext consumer
        messages.subscribe(System.out::println);

        // Subscriber
        messages.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("Subscribed!");
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Completed with an exception.");
            }

            @Override
            public void onComplete() {
                System.out.println("Completed!");
            }
        });
    }

    static class Client {
        private String name;
        private Integer age;

        public Client(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }
    }
}

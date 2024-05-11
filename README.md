# async-java

I'm starting my journey to brush up on my Java skills and for now I'll be focusing on concurrency 
and asynchronous programming in Java. As far as I remember, Java's native API has considerably improved
throughout the years and new features have been added to make it easier to work with concurrency.

I'll be logging my learnings here so that it serves as a reference for me in the future - given that I've
already taken this journey many years before.


### Thread pools

Thread pools in java mainly consist of two components: a bunch of threads and a queue (or multiple queues).

Requests are added to the queue and are eventually pulled out by the threads in the pool and processed. 

Thread pools in java can be either blocking or non-blocking.

#### One thread per request model / blocking I/O

In this threading model, there is one thread processing one request all the way to the end. 
When the processing requires data from another source (e.g an external api or a database), 
the thread will block (and do nothing) until the data is available - this is known as blocking i/o.

_The problem here is when all threads are busy processing requests and there are still requests waiting in the queue, as they will have to wait for threads to finish their current work before servicing them (it induce to a low level of responsiveness and latency)._

Servlets and JDBC are examples of common components using the one thread per request model (p.s though hardly developers use servlets
directly, they do use frameworks sitting one layer up the servlet api like Spring Web and Jersey).

* Implementation is usually simpler (and suitable for many use cases) but scalability is limited.

Latency: when many remote call are performed one after the other, the latency for that request will be the sum of all the latencies of the remote calls.

Isolation: slow services might impact other services as they hold onto a thread while they block (shared thread pool and/or cpu).

Scale up: as you want to serve more users, more threads have to be created. More threads running on the same machine the OS needs to perform more context switching and interruptions - thus less shared CPU time for threads. CPU cache is also hampered as it needs more switching (Modern CPUs make extensive use of CPU cache to reduce main memory access).

_the C10K problem - can your system/architecture handle 10k connected users at the same time?_


### Non blocking I/O

*Some concepts first*

Blocking vs Non-blocking and Synchronous vs Asynchronous are two different concepts. The first refers to OS concerns when performing I/O and either returning immediately 
or block waiting for the result to arrive. The second usually refers to control flow concerns - on a method call, will the caller have to wait until the method
completes or will the caller be released and can go off and do some other work.

The benefits of the non-blocking are related to the asynchronous threading model - it allows to get away from the one thread per request model approach.

*High level explanation*

The non-blocking approach uses an abstraction called selector, which associates a set of I/O events and multiplex them. The selector is then responsible
for monitoring when the OS (operational) events are ready, reading and iterating over them. Developers don't usually interact with the 
JVM low-level libs directly, but use higher-level abstractions and libs.

_It enables us to multiplex multiple requests, rather than keeping one thread per request._


#### Practical example

Under the `raw_handling` package, there are 2 servlets which perform two http requests to another servlet emulating a slow service (which just holds the response in 500 milliseconds). 
One of the servlets uses the blocking approach while the other the non-blocking.

To run the example, just execute the Main class in the Application class (main server starts on port 8080, slow responding service on 8081) and call the endpoints.

When benchmarking the performance with 1000 requests and 15 concurrent requests at the same time, the following results were obtained:

| Approach     | min    | mean   | 50th percentile | 80th percentile | 90th percentile | 99th percentile | max     |
|--------------|--------|--------|-----------------|-----------------|-----------------|-----------------|---------|
| Blocking     | 908 ms | 1486ms | 1016ms          | 2028ms          | 2540ms          | 5554ms          | 7588ms  |
| Non-blocking | 511ms  | 1627ms | 1518ms          | 2017ms          | 2020ms          | 2026ms          | 2529ms  |


Tests were performing using apache ab server benchmarking tool `ab -c 15 -n 1000 -k localhost:8080/blocking`

Notice (1) how the non-blocking approach tends to have more consistent response time and (2) how it's also more complex to implement, maintain and debug (especially when composing callbacks).


### Timeouts

Timeouts should be thought from a reliability point of view rather than performance. Setting a timeout is a way of ensuring that a request succeeded in a timely manner.

There a few ways to set a timeout in java:

1. In the asynchronous context in the servlet itself - it fails the entire operation if it goes above the defined time
2. On the request that goes to an external service - it fails only that specific request (helps with failure detection on an external service)

Timeouts should be based on real numbers / realistic expectations. You could use a function of the round-trip time with a service or a function of the expected latency budget for your end users.


### Retries

When failures are transient, you may want to retry the request instead of failing it. It's important to limit the number of retries to not overload the service (that might already be suffering with load).

It's extremely important to ensure that the operations being retried are idempotent.

### Circuit breakers

The circuit breaker monitors how many failing attempts are happening and when it reaches the defined threshold, it opens the circuit.

There are 3 states: closed, open and half-open.

Hystrix library and akka framework provide circuit breaker implementations.


## Threads history

### Thread API (1995)

Threads is a low-level concept (and API) which only provides things like start and join. Therefore, it is hard to write and maintain (it's been around since Java 1)
and doesn't help developers to write code that is close to the business problem statement.

Another problems are:
- one task coupled to one thread
- no control over how many threads are running (which could eventually undermine cpu performance)
- no way of prioritising tasks
- threads are disposable (not reused for another tasks). Creating threads is expensive
- runnable interface does return anything (void) therefore it's hard to get the result


#### Samples

1. Spawning a thread without getting the result

```java

    var thread = new Thread(() -> { // java 8 lambda syntax to create a runnable, otherwise an implementation of the Runnable interface or extension of the Thread class
        PriceService.getPrice(productName);
    });
    thread.start();
```

However, more than often it's necessary to get the result. And this is when things get hairy as a stateful object is required.

2. Spawning a thread and working around the void return from the runnable implementation

```java

    class PriceFetcher implements Runnable {
        private final String productName;
        public BigDecimal price; // deliberately public
        
        public PriceFetcher(String productName) {
            this.productName = productName;
        }
        
        @Override
        public void run() {
            this.price = PriceService.getPrice(productName);
        }
    }

    ...
    var priceFetcher = new PriceFetcher(productName);
    var thread = new Thread(priceFetcher);
    thread.run();
    thread.join(); // block until the thread finishes
    
    priceFetcher.price; // get the result
```


### Executor service and Future (2005)

It's been introduced in java 5. It's main goal was to decouple the task submission and the task execution following a producer-consumer architecture. 
It also introduced the thread pool concept where threads are reused for multiple tasks, saving the creation and destruction overhead.

There are many executor services available in java like ForkJoinPool (used by the stream api), ThreadPoolExecutor and ScheduledThreadPoolExecutor - however
the executor service is the general abstraction and allows you to define things like the queue size, number of threads, rejection policies, etc.

Along with the ExecutorService, the Future and Callable interfaces were introduced to allow the retrieval of the execution result. It is like a promise in javascript, 
where there is a promise that the result will be available in the future.

_future, promise and deferred are all similar concepts but with different names in different languages._

```java

    var executor = Executors.newFixedThreadPool(10);
    Future<Double> future = executor.submit(() -> PriceService.getPrice(productName));
    var price = future.get(); // block until the result is available
    var price = future.get(2, TimeUnit.SECONDS); // with timeout

```

Even though executor services were a much more powerful api than threads, and gave another level of abstraction for concurrency, they were not perfect.

Composing multiple futures responses was hard and did not help with the declarative / functional programming style introduced 
by java 8 (it just provided a get method) where it's possible to pipe inputs and outputs. Furthermore, to get the result of a future,
it's necessary to block the thread - eat up resources doing nothing.

Listenable future, introduced by guava, was an attempt to solve the problem of composing future responses and introduce callbacks to solve the blocking issue - 
however it didn't pipe either.


### Completable Future (2014)

The biggest difference here is that the client can define handlers, which are executed when the future completes, instead of blocking waiting for the result.

API in java are using completable future like Process API, HttpClient, Spring controllers, AKKA, etc.

Ways of creating futures:

```java

    CompletableFuture<Price> future = new CompletableFuture<>(); // 1 - you instantiate it yourself and then complete it from an async service 
    // (future.complete(42); or future.completeExceptionally(new TimeoutException());). In this case you are responsible for completing the future
    // and you are basically declaring to the client that you owe them a result.
    
    ComparableFuture<Price> price = CompletableFuture.supplyAsync(() -> PriceService.getPrice(productName)); // 2 - somewhat similar to executor service
    // where you send a task to be executed.

```

Important to highlight that by default supplyAsync uses the common fork join pool, however there is an overloaded method that allows you to
specify which executor service (thread pool) to use.

New methods (idiomatic operations) - specified by the fluent-style api CompletionStage:

1. future.thenAccept(value -> System.out.println(value)); // callback for when the future completes AND you need the result
2. future.thenRun(() -> System.out.println("done")); // callback for when the future completes AND you don't need the result
3. future.thenCompose(value -> CompletableFuture.supplyAsync(() -> someElse()); // meant for async mapping - extremely powerful especially if the method being called is also non-blocking
4. future.thenApply(value -> value * 2); // meant for sync mapping (essential difference from thenCompose)

This fluent-style api allows us to specify the problem statement we are working on in a very declarative manner (and easily compose).

Besides the CompletionStage interface, CompleteFuture also implements the Future interface, therefore it also provides the old blocking methods.

CompletableFuture is modelled with 3 states: value available, the value is not available yet, completed with exceptions (this last one is particularly 
an improvement over the other method).

_Fork Join Pool is used by parallel stream, therefore using it for completable future can be problematic (if the completable future is performing underlying blocking work)_
_Fork Join Pool size is determined by the number of available cores in the machine, thus blocking its threads causing it to be undersized_

#### Handling Exceptions

CF has a way of nicely handling completable future that completes with an exception. It's great because it allows developers to define recovery mechanisms.

There are 3 methods to handle exceptions:

```java
    // allows to handle an exception and return a value
    future.exceptionally(exception -> {
            // recovery mechanism
            // return a default / processed value 
    }).thenAccept(value -> {... });

    // gives the option of dealing with the failure and the value scenario
    future.whenComplete((value, exception) -> {
        if (exception != null) {
            ...
        } else {
            ...    
        }
    })

    future.handle(...) // research more.

```

#### Sequence patterns

Up to now, we are only able to combine two futures. However, there are scenarios where you need to combine many futures.

There is a couple of operations available to do so:

1. anyOf - returns the result of the first future that completes (the only detail is that it returns a completable future of object, as not all the futures might return the same type) 
2. allOf - when all futures complete, it return a completable future of void (as it doesn't return anything :( not super useful tho)

_If you want to get the result of all completable futures, you have to right a solution yourself_

```java

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allFuturesDone =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allFuturesDone.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(toList()));
    }
```

## Reactive Streams - RxJava

This is quite a buzz word nowadays and there are different meaning of what reactive means:

1. Functional reactive programming (FRP) - this is more the academic perspective on reactive programming (it is a programming paradigm that combines functional programming with reactive programming concepts)
2. Reactive systems - is more about the architectural style. A way of getting systems and components and allowing them to work together following the _reactive manifesto***_ principles. 
3. Reactive programming - it's a declarative and event-based style of programming. Decompose a big problem into multiple async steps then composing them together.

***reactive manifesto defines a key set of properties that an application has to follow in order to be reactive: responsive, resilient, elastic and message-driven.

- responsive: when a user makes a request, the system should respond in a timely (and effective) manner.
- resilient: when things go wrong, the system doesn't collapse as a house of card and it continues to respond to users
- elastic: as the load/demand increases (request rate goes up), the resources are added to the system to handle the load - the same applies when the load decreases, resources are removed.
- message-driven: this property is meant to enable the other 3.

_Reactive programming has a few models: callbacks, actors, completable future and reactive streams. One can achieve reactive systems by using purely callbacks_

Why yet another library? The benefits of asynchronous programming and enabling non-blocking i/o are clear by now. However *callbacks* are really low-level thus 
hard to develop and maintain. *Completable future* only deals nicely with single values and frequently we want to process series of values / sequence of events. 
*Actors* require getting deep into the whole framework, which is great for some use cases but not all - and it also slightly low-level.

Reactive stream libs like rxjava offer a concept of a stream of async and non-blocking sequence of events which is either an observable or a flowable.
It can be seem as a pipe construct, where declarative sequences of operations are defined to be executed when the data arrives.

One way of thinking about observables (or flowables) is in comparison with the blocking approach:

_Observable rxjava 1st release, flowable rxjava 2nd release_

|              | Single Item          | Many Items                      |
|--------------|----------------------|---------------------------------|
| Blocking     | T get()              | Stream<T> (or List<T>)          |
| Non-blocking | CompletableFuture<T> | Observable<T> (or Flowable<T>)  |

Stream is more suitable for computational tasks, while observable supports composing, blocking and non blocking i/o sources (more flexibility in its threading model)
- batch computation vs on demand computation.

Ps: observable clients treat it as non-blocking even though the source may block - similarly to completable feature, it's down to the underlaying implementation.
.

##### Observer Design Pattern vs Observable reactive streams

_Refresher: Observer design pattern is a way of broadcasting events to different consumers_

They have similar names and are for similar purposes. However, the observable has a stream like API with common operations, which if you want to achieve on 
with the observer you have to implement yourself, including potential thread safety issues. 

Observable also has a few more features like backpressure, throttling, error handling, etc.


#### Lifecycle

The lifecycle is defined by the subscriber interface and it's composed of 4 methods: onSubscribe, onNext, onError and onComplete.

onSubscribe is the only one that warrants an explanation as the others are self-explanatory. Upon subscription, the subscriber receives a Subscription object,
which contains 2 methods - request and cancel. The request method is used to request a number of items from the publisher (backpressure).

But many times developers use the consumer interface instead and provide different consumers for each of the events (onNext, onError, onComplete).

Example in the class `com.cedran.async_java.reactive_streams.Examples#subscriberExample`.

#### Types of flowables

1. Hot flowables - they start emitting events as soon as they are created, regardless of whether there are subscribers or not (subscribes may miss events) - The publisher controls the rate of events.
Examples: mouse events (no control over the user controlling the hardware), stock prices (no control over the stock market), stream of video being broadcast
2. Cold flowables - they start emitting events only when there is a subscriber (subscribers will receive all events) - only want suscriber
Examples: db query, file i/o, http request


#### Unit Tests

There is a built-in test subscriber in RX Java API. TestSubscriber implements Subscriber interface and can be used to subscribe to objects.

The test subscriber has an api to allow developers to make asserts on the events that are going through the flowable.

`assertValue`, `assertValues`, `assertValueCount`, `assertSubscribed`, `assertNotSubscribed`, `assertComplete`, `assertError`, `assertTerminated` are just a few of the assertions available.


#### Error Handling

Errors can be categorised in 3 types: transient failure (network issue, remote service failure), 
calculation / processing / logic error (invalid user input, divide by zero), unrecoverable error (out of memory, disk failure, disk space - somebody has to intervene). 

Strategies for error handling:
- Transient failure - retry
- processing errors - default value (e.g notify the user that a given value is invalid)
- unrecoverable error - propagate the errors somewhere (so an alert can be sent)

`onErrorReturn(ex)`, `onErrorReturnItem()`, `onErrorResumeNext(anotherFlowableToContinue)`.

Important to bear in mind that once an exception occurs, the flowable is terminated and no more events are emitted (besides the default value).
If you want to continue processing, you have to use `onErrorResumeNext`, which provides a new flowable to continue processing.

#### Backpressure

Backpressure is the process of controlling the rate at which something occurs in order to ensure that there isn't breach of capacity limit
(and starts acting in a unstable or potentially dangerous manner).

Queues are a good example of backpressure - however if the producers are producing at a faster rate that the consumers can consume, 
and the queue has an unbounded size, eventually memory leak will occur.

Cold flowables can be backpressured.




#### Throttling (conflation)

Sometimes you don't have the control over the producer and in order to avoid overloading your system, you may prefer to drop some events 
(example, overflow valve in a pipe that conducts water to avoid pipe bursting) - useful when you cannot backpressure (control the rate that something is produced).

Hot flowables can be throttled.

One way of throttling is by using the `sample` operator (`throttleLast` basically does the same). It takes all the values within the 
specified period and emits the last one (dropping the rest).

Another option is the `buffer` operator. It aggregates the number of requested elements into a buffer and then emmit a list of those values.


#### Specialised Thread Pools

There are different types of thread pools that can be used for different purposes. RxJava provides a scheduler abstraction to allow developers
to determine which thread pool a given flowable and / or operator should run on (and even ether the should run with a delay or periodically).

Some of the API methods are overloaded with variations that take schedulers as parameters (some others simply define the scheduler by themselves).

```java
    loadUsersFromDb()
        .observeOn(Schedulers.computation())
        .map(this::calculateAnalytics)
        .subscribeOn(Schedulers.io())
        .subscribe(this::storeToDb);
```

`observeOn` defines where next operators should run on

`subscribeOn` defines where the subscription should run on (until it finds a `observeOn` so that from there on it runs on the `observeOn` scheduler).
The position of the subscribeOn in the chain doesn't matter - it'll apply from the first operator. If there are 2 subscribeOn, the first one is applied 
(but there shouldn't be more than one).

Common schedulers: `io`, `computation` and `trampoline`


#### Processors

F





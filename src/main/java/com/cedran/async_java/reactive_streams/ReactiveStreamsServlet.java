package com.cedran.async_java.reactive_streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ReactiveStreamsServlet extends HttpServlet {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        long start = System.currentTimeMillis();
        var asyncContext = req.startAsync();
        var flowable1 = getSlowResponse();
        var flowable2 = getSlowResponse();
        var flowable3 = getBlockingSlowResponse(); // notice the difference with the other calls

        Flowable.zip(flowable1, flowable2, flowable3, (r1, r2, r3) -> {
            long zipStart = System.currentTimeMillis();
            try {
                var response = (HttpServletResponse) asyncContext.getResponse();
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                response.getWriter().write(mapper.writeValueAsString(new Object[]{r1, r2, r3}));
                asyncContext.complete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Time taken (zip): " + (System.currentTimeMillis() - zipStart) + "ms");
            return r1;
        }).subscribe(); // notice the difference with blockingSubscribe how long the thread executing the servlet is active

        System.out.println("Time taken (doGet): " + (System.currentTimeMillis() - start) + "ms");
    }

    public Flowable<Object> getSlowResponse() {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8081/slow-downstream-service")).build();
            var f = Flowable.create(emitter -> {
                long emitterStart = System.currentTimeMillis();
                HttpClient.newHttpClient()
                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(r -> r.body())
                        .thenAccept(response -> {
                            if (!emitter.isCancelled()) {
                                emitter.onNext(response);
                                emitter.onComplete();
                            }
                        });
                System.out.println("Time taken (emitter): " + (System.currentTimeMillis() - emitterStart) + "ms");
            }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.io());
            System.out.println("Time taken (getSlow): " + (System.currentTimeMillis() - start) + "ms");
            return f;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Flowable<Object> getBlockingSlowResponse() {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8081/slow-downstream-service")).build();
            var f = Flowable.create(emitter -> {
                long emitterStart = System.currentTimeMillis();
                var response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                if (!emitter.isCancelled()) {
                    emitter.onNext(response.body());
                    emitter.onComplete();
                }
                System.out.println("Time taken (emitter): " + (System.currentTimeMillis() - emitterStart) + "ms");
            }, BackpressureStrategy.LATEST);
            System.out.println("Time taken (getSlow): " + (System.currentTimeMillis() - start) + "ms");
            return f;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

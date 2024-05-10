package com.cedran.async_java.raw_handling;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class NonBlockingServlet extends HttpServlet {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            var request = HttpRequest.newBuilder(new URI("http://localhost:8081/slow-downstream-service")).build();

            var asyncContext = req.startAsync();
            var responseHandler = new ComposedCallback(asyncContext);
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply(HttpResponse::body).thenAccept(responseHandler::onResponse1);
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApply(HttpResponse::body).thenAccept(responseHandler::onResponse2);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public class ComposedCallback {
        private Map<String, Object> response1;
        private Map<String, Object> response2;
        private AsyncContext asyncContext;

        public ComposedCallback(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
        }

        public synchronized void onResponse1(InputStream response) {
            this.response1 = parseResponse(response);
            this.response1.put("response", 1);
            checkResponses();
        }

        public synchronized void onResponse2(InputStream response) {
            this.response2 = parseResponse(response);
            this.response2.put("response", 2);
            checkResponses();
        }

        private Map<String, Object> parseResponse(InputStream response) {
            try {
                var typeReference = new TypeReference<HashMap<String, Object>>() {};
                return mapper.readValue(response, typeReference);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void checkResponses() {
            if (response1 != null && response2 != null) {
                var res = (HttpServletResponse) this.asyncContext.getResponse();
                res.setStatus(HttpServletResponse.SC_OK);
                res.setContentType("application/json");
                try {
                    mapper.writer().writeValue(res.getOutputStream(), Map.of("cool", this.response1, "cool2", this.response2));
                } catch (IOException e) {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                asyncContext.complete();
            }
        }
    }
}

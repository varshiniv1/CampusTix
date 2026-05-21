package com.university.campustix;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrencyStressTest {

    @Test
    public void simulateHighTraffic() throws InterruptedException {
        int numberOfRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 0; i < numberOfRequests; i++) {
            String studentId = "student_" + i;
            executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/v1/tickets/claim?studentId=" + studentId + "&seatId=1"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();

                    client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        Thread.sleep(5000); // Give Kafka time to process
    }
}
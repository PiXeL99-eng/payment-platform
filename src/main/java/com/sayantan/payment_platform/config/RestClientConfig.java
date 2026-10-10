package com.sayantan.payment_platform.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration  // Annotation tells this class provides application setup
public class RestClientConfig {
    
    @Bean   // Annotation to create one reusable RestClient object at Startup
    public RestClient mockPaymentGatewayRestClient(
        @Value("${payment.gateway.base-url}") String gatewayBaseUrl // Takes gatewayBaseUrl from application.properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))  // 2 seconds timeout for the new connection to start
        .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(3));   // Response should arrive within 3 seconds

        return RestClient.builder()
        .baseUrl(gatewayBaseUrl)    // means clients can use /gateway/payments, rather than repeating http://localhost:8081 everywhere
        .requestFactory(requestFactory)
        .build();
    }
}

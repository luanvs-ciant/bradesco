package br.com.empresa.bff.observability;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final String HEADER_NAME = "X-Correlation-ID";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            request.getHeaders().set(HEADER_NAME, correlationId);
        }
        return execution.execute(request, body);
    }
}

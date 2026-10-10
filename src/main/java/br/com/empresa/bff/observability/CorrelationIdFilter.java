package br.com.empresa.bff.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String suppliedId = request.getHeader(CorrelationId.HEADER);
        String correlationId = suppliedId != null && VALID_ID.matcher(suppliedId).matches()
                ? suppliedId : UUID.randomUUID().toString();
        String previousId = MDC.get(CorrelationId.MDC_KEY);
        long startedAt = System.nanoTime();

        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            logger.atInfo().addKeyValue("event", "http_request_completed")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("traceId", correlationId)
                    .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                    .log("Requisição finalizada");
            if (previousId == null) {
                MDC.remove(CorrelationId.MDC_KEY);
            } else {
                MDC.put(CorrelationId.MDC_KEY, previousId);
            }
        }
    }
}

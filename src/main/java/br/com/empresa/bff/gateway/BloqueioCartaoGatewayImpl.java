package br.com.empresa.bff.gateway;

import java.net.SocketTimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BloqueioCartaoGatewayImpl implements BloqueioCartaoGateway {

    private final BloqueioCartaoClient client;

    @Override
    public BloqueioCartaoDownstreamResponse bloquear(BloqueioCartao bloqueio) {
        try {

            return client.bloquear(bloqueio);

        } catch (DownstreamIntegrationException exception) {
            throw exception;

        } catch (RetryableException exception) {

            if (exception.getCause() instanceof SocketTimeoutException) {
                throw new DownstreamIntegrationException(HttpStatus.GATEWAY_TIMEOUT,
                        "Tempo limite do serviço de bloqueio excedido", exception);
            }
            throw new DownstreamIntegrationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de bloqueio indisponível", exception);

        } catch (RuntimeException exception) {
            throw new DownstreamIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Resposta inválida do serviço de bloqueio", exception);
        }
    }
}

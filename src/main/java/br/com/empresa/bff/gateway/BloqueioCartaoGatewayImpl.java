package br.com.empresa.bff.gateway;

import java.net.SocketTimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;
import feign.RetryableException;

@Component
public class BloqueioCartaoGatewayImpl implements BloqueioCartaoGateway {

    private final BloqueioCartaoClient client;
    private final BloqueioCartaoGatewayMapper mapper;

    public BloqueioCartaoGatewayImpl(
            BloqueioCartaoClient client,
            BloqueioCartaoGatewayMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public ResultadoBloqueioCartao bloquear(BloqueioCartao bloqueio) {
        var request = mapper.toDownstreamRequest(bloqueio);

        try {
            var response = client.bloquear(request);

            return mapper.toResult(response);
        } catch (DownstreamIntegrationException exception) {
            throw exception;
        } catch (RetryableException exception) {
            if (exception.getCause() instanceof SocketTimeoutException) {
                throw new DownstreamIntegrationException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Tempo limite do serviço de bloqueio excedido");
            }
            throw new DownstreamIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "Serviço de bloqueio indisponível");
        } catch (RuntimeException exception) {
            throw new DownstreamIntegrationException(
                    HttpStatus.BAD_GATEWAY,
                    "Serviço de bloqueio indisponível");
        }
    }
}

package br.com.empresa.bff.gateway;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import br.com.empresa.bff.domain.gateway.BloqueioCartaoGateway;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.exception.DownstreamIntegrationException;
import br.com.empresa.bff.gateway.client.BloqueioCartaoClient;
import br.com.empresa.bff.gateway.mapper.BloqueioCartaoGatewayMapper;

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
        } catch (RuntimeException exception) {
            if (exception instanceof DownstreamIntegrationException) {
                throw exception;
            }
            throw new DownstreamIntegrationException(
                    HttpStatusCode.valueOf(502),
                    "Serviço de bloqueio indisponível");
        }
    }
}

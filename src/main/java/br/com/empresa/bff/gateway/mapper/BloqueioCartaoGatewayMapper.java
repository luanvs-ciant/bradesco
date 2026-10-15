package br.com.empresa.bff.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest.PortadorDownstreamRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BloqueioCartaoGatewayMapper {

    BloqueioCartaoDownstreamRequest toDownstreamRequest(BloqueioCartao bloqueio);

    @Mapping(target = "portadorId", source = "id")
    PortadorDownstreamRequest toPortadorDownstreamRequest(Portador portador);
}

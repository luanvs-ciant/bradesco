package br.com.empresa.bff.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.com.empresa.bff.domain.model.ResultadoBloqueioCartao;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BloqueioCartaoDownstreamResponseMapper {

    ResultadoBloqueioCartao toResult(BloqueioCartaoDownstreamResponse response);
}

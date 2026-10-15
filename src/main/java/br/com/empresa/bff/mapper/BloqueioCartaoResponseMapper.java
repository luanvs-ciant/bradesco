package br.com.empresa.bff.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BloqueioCartaoResponseMapper {

    BloqueioCartaoResponse toResponse(BloqueioCartaoDownstreamResponse response);
}

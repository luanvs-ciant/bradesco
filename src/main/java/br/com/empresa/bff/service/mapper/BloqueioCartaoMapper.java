package br.com.empresa.bff.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.response.BloqueioCartaoResponse;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamRequest;
import br.com.empresa.bff.gateway.dto.BloqueioCartaoDownstreamResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BloqueioCartaoMapper {

    BloqueioCartaoDownstreamRequest toDownstreamRequest(BloqueioCartaoRequest request);

    BloqueioCartaoResponse toResponse(BloqueioCartaoDownstreamResponse response);
}

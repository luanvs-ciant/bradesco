package br.com.empresa.bff.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import br.com.empresa.bff.controller.dto.request.BloqueioCartaoRequest;
import br.com.empresa.bff.controller.dto.request.PortadorRequest;
import br.com.empresa.bff.domain.model.BloqueioCartao;
import br.com.empresa.bff.domain.model.Portador;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BloqueioCartaoMapper {

    BloqueioCartao toDomain(BloqueioCartaoRequest request);

    @Mapping(target = "id", source = "portadorId")
    Portador toPortador(PortadorRequest request);
}

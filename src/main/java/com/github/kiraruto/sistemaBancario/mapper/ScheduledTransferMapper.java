package com.github.kiraruto.sistemaBancario.mapper;

import com.github.kiraruto.sistemaBancario.dto.ScheduledTransferDTO;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduledTransferMapper {
    ScheduledTransfer toEntity(ScheduledTransferDTO dto);
    ScheduledTransferDTO toDto(ScheduledTransfer entity);
}

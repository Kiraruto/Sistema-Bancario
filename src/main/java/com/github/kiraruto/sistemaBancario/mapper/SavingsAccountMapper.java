package com.github.kiraruto.sistemaBancario.mapper;

import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavingsAccountMapper {

    @Mapping(target = "isActive", constant = "true")
    SavingsAccount toEntity(SavingsAccountDTO dto);

    SavingsAccountDTO toDto(SavingsAccount entity);
}

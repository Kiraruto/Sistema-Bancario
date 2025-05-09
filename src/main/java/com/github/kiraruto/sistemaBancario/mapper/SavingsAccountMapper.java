package com.github.kiraruto.sistemaBancario.mapper;

import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SavingsAccountMapper {
    SavingsAccount toEntity(SavingsAccountDTO dto);
    SavingsAccountDTO toDto(SavingsAccount entity);
}

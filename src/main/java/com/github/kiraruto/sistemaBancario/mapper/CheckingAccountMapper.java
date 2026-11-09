package com.github.kiraruto.sistemaBancario.mapper;

import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CheckingAccountMapper {
    CheckingAccount toEntity(CheckingAccountDTO dto);

    CheckingAccountDTO toDto(CheckingAccount entity);
}

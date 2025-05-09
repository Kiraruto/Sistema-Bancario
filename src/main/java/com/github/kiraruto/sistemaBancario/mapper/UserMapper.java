package com.github.kiraruto.sistemaBancario.mapper;

import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserDTO dto);
    UserDTO toDto(User entity);
}

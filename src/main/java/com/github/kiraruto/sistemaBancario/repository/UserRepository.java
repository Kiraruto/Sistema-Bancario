package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u.isActive FROM User u WHERE u.id = :id")
    Boolean isActiveById(@Param("id") UUID id);

    List<User> findAllByIsActiveTrue();

    List<User> findAllById(UUID id);

    Optional<User> findByEmail(String email);

    User findByRole(EnumUserRole enumUserRole);
}

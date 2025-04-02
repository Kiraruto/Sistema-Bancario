package com.github.kiraruto.sistemaBancario.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kiraruto.sistemaBancario.dto.UserDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;  // Nome de usuário

    @Column(name = "first_name", unique = true, nullable = false, length = 50)
    private String firstName; // primeiro nome

    @Column(name = "last_name", unique = true, nullable = false, length = 50)
    private String lastName; // ultimo nome

    @Column(name = "password", nullable = false)
    private String password;  // Senha

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;  // Email do usuário

    @Column(name = "role", length = 8)
    @Enumerated(EnumType.STRING)
    private EnumUserRole role; // Tipo de usuario

    @Column(name = "is_active")
    private boolean isActive;  // Indica se o usuário está ativo ou não

    @OneToOne
    @JoinColumn(name = "checking_account")
    private CheckingAccount checkingAccount;

    @OneToOne
    @JoinColumn(name = "savings_account")
    private SavingsAccount savingsAccount;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<ScheduledTransfer> scheduledTransfer;

    public User(UserDTO userDTO, String encode) {
        this.username = userDTO.username();
        this.firstName = userDTO.firstName();
        this.lastName = userDTO.lastName();
        this.password = encode;
        this.email = userDTO.email();
        this.role = EnumUserRole.CLIENTE;
        this.isActive = true;
    }

    @JsonCreator
    public User(@JsonProperty("id") UUID id) {
        this.id = id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

package com.github.kiraruto.sistemaBancario.config;

import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private static final String[] PUBLIC_PATHS = {
            "/authentication/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String[] ADMIN_PATHS = {
            "/users",
            "/users/**",
            "/users/{id}/admin",
            "/users/{id}/gerente",
            "/users/{id}/cliente",
            "/users/{id}/disableUser",
            "/users/{id}/activateUser",
            "/transaction/**"
    };

    private static final String[] GERENTE_PATHS = {
            "/savings-accounts",
            "/savings-accounts/**",
            "/checking-accounts",
            "/checking-accounts/**",
            "/users/{id}/update",
            "/schedule",
            "/schedule/**"
    };

    private static final String[] CLIENTE_PATHS = {
            "/savings-accounts/{id}",
            "/savings-accounts/{id}/transactions",
            "/savings-accounts/{id}/balance",
            "/savings-accounts/{id}/withdraw",
            "/checking-accounts/{id}",
            "/checking-accounts/{id}/transactions",
            "/checking-accounts/{id}/balance",
            "/checking-accounts/{id}/withdraw",
            "/transaction/**"
    };

    private static final String[] ADMIN_GERENTE_PATHS = {
            "/users/{id}/disableUser",
            "/users/{id}/activateUser",
            "/savings-accounts",
            "/checking-accounts",
            "/schedule/**"
    };

    private static final String[] ADMIN_CLIENTE_PATHS = {
            "/users/{id}/cliente",
            "/transaction/**",
            "/savings-accounts/{id}/balance",
            "/checking-accounts/{id}/balance"
    };

    private static final String[] GERENTE_CLIENTE_PATHS = {
            "/savings-accounts/{id}/withdraw",
            "/savings-accounts/{id}/withdrawal",
            "/savings-accounts/transfer/savingsAccount",
            "/checking-accounts/{id}/withdraw",
            "/checking-accounts/{id}/withdrawal",
            "/checking-accounts/transfer/checkingAccount",
            "/schedule"
    };

    private static final String[] ADMIN_GERENTE_CLIENTE_PATHS = {
            "/users/{id}/update",
            "/transaction/**"
    };

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(ADMIN_PATHS).hasAuthority(EnumUserRole.ADMIN.name())
                        .requestMatchers(GERENTE_PATHS).hasAuthority(EnumUserRole.GERENTE.name())
                        .requestMatchers(CLIENTE_PATHS).hasAuthority(EnumUserRole.CLIENTE.name())
                        .requestMatchers(ADMIN_GERENTE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.GERENTE.name())
                        .requestMatchers(ADMIN_CLIENTE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.CLIENTE.name())
                        .requestMatchers(GERENTE_CLIENTE_PATHS).hasAnyAuthority(EnumUserRole.GERENTE.name(), EnumUserRole.CLIENTE.name())
                        .requestMatchers(ADMIN_GERENTE_CLIENTE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.GERENTE.name(), EnumUserRole.CLIENTE.name())
                        .anyRequest().authenticated())
                .sessionManagement(maneger -> maneger.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider()).addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
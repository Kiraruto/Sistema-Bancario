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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private static final String[] PUBLIC_PATHS = {
            "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Rotas literais que precisam vir antes dos padroes "/{id}" para nao serem
     * capturadas por eles (ex.: "/savings-accounts/{id}" casaria com "/savings-accounts/create").
     */
    private static final String[] ADMIN_GERENTE_CREATE_PATHS = {
            "/savings-accounts/create",
            "/checking-accounts/create"
    };

    /**
     * Rotas de conta usadas pelos tres perfis. Precisam vir antes dos matchers
     * amplos de ADMIN/GERENTE para que o CLIENTE nao seja bloqueado por eles.
     */
    private static final String[] ALL_ROLES_PATHS = {
            "/users/{id}/update",

            "/savings-accounts/{id}",
            "/savings-accounts/{id}/transactions",
            "/savings-accounts/{id}/balance",
            "/savings-accounts/{id}/deposit",
            "/savings-accounts/{id}/withdrawal",
            "/savings-accounts/transfer/savingsAccount",

            "/checking-accounts/{id}",
            "/checking-accounts/{id}/transactions",
            "/checking-accounts/{id}/balance",
            "/checking-accounts/{id}/deposit",
            "/checking-accounts/{id}/withdrawal",
            "/checking-accounts/transfer/checkingAccount",

            "/transfer"
    };

    private static final String[] ADMIN_CLIENTE_PATHS = {
            "/users/{id}/cliente"
    };

    private static final String[] ADMIN_PATHS = {
            "/users",
            "/users/**",
            "/transaction/**"
    };

    private static final String[] ADMIN_GERENTE_PATHS = {
            "/savings-accounts",
            "/savings-accounts/**",
            "/checking-accounts",
            "/checking-accounts/**",
            "/schedule",
            "/schedule/**"
    };

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(ADMIN_GERENTE_CREATE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.GERENTE.name())
                        .requestMatchers(ALL_ROLES_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.GERENTE.name(), EnumUserRole.CLIENTE.name())
                        .requestMatchers(ADMIN_CLIENTE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.CLIENTE.name())
                        .requestMatchers(ADMIN_PATHS).hasAuthority(EnumUserRole.ADMIN.name())
                        .requestMatchers(ADMIN_GERENTE_PATHS).hasAnyAuthority(EnumUserRole.ADMIN.name(), EnumUserRole.GERENTE.name())
                        .anyRequest().authenticated())
                .sessionManagement(maneger -> maneger.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Não autorizado. Faça login para acessar este recurso.\"}");
        };
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

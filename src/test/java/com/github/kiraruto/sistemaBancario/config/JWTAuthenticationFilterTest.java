package com.github.kiraruto.sistemaBancario.config;

import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.service.CustomUserDetailsService;
import com.github.kiraruto.sistemaBancario.service.JWTService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regressão do P1: token malformado não pode escapar do filtro como 500.
 * A requisição deve seguir sem autenticação, deixando o Spring Security
 * responder 401 nas rotas protegidas.
 */
@DisplayName("JWTAuthenticationFilter - tokens inválidos")
class JWTAuthenticationFilterTest {

    private final JWTService jwtService = mock(JWTService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JWTAuthenticationFilter filter =
            new JWTAuthenticationFilter(jwtService, userDetailsService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("token malformado deve seguir a cadeia sem autenticar")
    void malformedTokenShouldNotBreakRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-malformado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserName("token-malformado"))
                .thenThrow(new MalformedJwtException("token inválido"));

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "A requisição deve continuar na cadeia de filtros");
        assertNull(
                SecurityContextHolder.getContext().getAuthentication(),
                "Um token inválido não pode autenticar a requisição"
        );
    }

    @Test
    @DisplayName("header Bearer sem token não deve quebrar o filtro")
    void bearerWithoutTokenShouldNotBreakRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("token de usuário desativado não deve autenticar")
    void disabledUserTokenShouldNotAuthenticate() throws Exception {
        User user = new User();
        user.setUsername("desativado");
        user.setRole(EnumUserRole.CLIENTE);
        user.setActive(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserName("token-valido")).thenReturn("desativado");
        when(userDetailsService.loadUserByUsername("desativado")).thenReturn(user);
        when(jwtService.isTokenValid("token-valido", user)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("token válido de usuário ativo deve autenticar")
    void validTokenShouldAuthenticate() throws Exception {
        User user = new User();
        user.setUsername("ativo");
        user.setRole(EnumUserRole.CLIENTE);
        user.setActive(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserName("token-valido")).thenReturn("ativo");
        when(userDetailsService.loadUserByUsername("ativo")).thenReturn(user);
        when(jwtService.isTokenValid("token-valido", user)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}

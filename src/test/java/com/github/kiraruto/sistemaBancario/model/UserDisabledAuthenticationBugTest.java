package com.github.kiraruto.sistemaBancario.model;
import com.github.kiraruto.sistemaBancario.model.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Candidato A: usuario desativado continua autenticando.
 *
 * Contrato esperado: UserService.disableUser marca isActive=false; o Spring Security
 * consulta UserDetails.isEnabled() para bloquear o login. O codigo atual sobrescreve
 * isEnabled() retornando sempre true, anulando a desativacao.
 */
class UserDisabledAuthenticationBugTest {

    @Test
    void disabledUserShouldNotBeEnabledForAuthentication() {
        User user = new User();
        user.setActive(false);

        assertFalse(
                user.isEnabled(),
                "Usuario com isActive=false nao deveria estar habilitado para autenticar"
        );
    }
}

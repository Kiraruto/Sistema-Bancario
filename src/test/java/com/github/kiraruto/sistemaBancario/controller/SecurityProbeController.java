package com.github.kiraruto.sistemaBancario.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de sonda usado apenas para testar a precedencia dos matchers de seguranca.
 *
 * Fica sob o pacote base para que o @WebMvcTest o encontre, mas so existe no profile
 * "security-probe" para nao ser registrado pelos contextos completos de @SpringBootTest.
 */
@RestController
@Profile("security-probe")
public class SecurityProbeController {

    @GetMapping("/checking-accounts/{id}/deposit")
    public String checkingDeposit(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/balance")
    public String savingsBalance(@PathVariable String id) {
        return "ok";
    }

    @PutMapping("/users/{id}/update")
    public String updateUser(@PathVariable String id) {
        return "ok";
    }
}

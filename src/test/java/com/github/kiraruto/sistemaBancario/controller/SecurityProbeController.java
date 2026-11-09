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

    @GetMapping("/checking-accounts")
    public String listChecking() {
        return "ok";
    }

    @GetMapping("/savings-accounts")
    public String listSavings() {
        return "ok";
    }

    @GetMapping("/checking-accounts/{id}")
    public String checkingById(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}")
    public String savingsById(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/checking-accounts/{id}/transactions")
    public String checkingTransactions(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/transactions")
    public String savingsTransactions(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/checking-accounts/{id}/deposit")
    public String checkingDeposit(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/deposit")
    public String savingsDeposit(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/checking-accounts/{id}/withdrawal")
    public String checkingWithdrawal(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/withdrawal")
    public String savingsWithdrawal(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/balance")
    public String savingsBalance(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/checking-accounts/{id}/balance")
    public String checkingBalance(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/transfer")
    public String transfer() {
        return "ok";
    }

    @GetMapping("/savings-accounts/{id}/disableSavingsAccount")
    public String disableSavings(@PathVariable String id) {
        return "ok";
    }

    @GetMapping("/users")
    public String listUsers() {
        return "ok";
    }

    @GetMapping("/users/{id}/cliente")
    public String userCliente(@PathVariable String id) {
        return "ok";
    }

    @PutMapping("/users/{id}/update")
    public String updateUser(@PathVariable String id) {
        return "ok";
    }
}

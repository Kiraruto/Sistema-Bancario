package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountAccessService {

    private final CheckingAccountRepository checkingAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public void checkCheckingAccountAccess(UUID accountId) {
        if (isStaff()) {
            return;
        }

        CheckingAccount account = checkingAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta corrente não encontrada"));

        checkOwnership(account.getUser());
    }

    public void checkSavingsAccountAccess(UUID accountId) {
        if (isStaff()) {
            return;
        }

        SavingsAccount account = savingsAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Conta poupança não encontrada"));

        checkOwnership(account.getUser());
    }

    private boolean isStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (EnumUserRole.ADMIN.name().equals(role)
                    || EnumUserRole.GERENTE.name().equals(role)) {
                return true;
            }
        }

        return false;
    }

    private void checkOwnership(User owner) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication != null ? authentication.getName() : null;

        if (owner == null
                || authenticatedUsername == null
                || !authenticatedUsername.equals(owner.getUsername())) {
            throw new AccessDeniedException("Você não tem permissão para acessar esta conta");
        }
    }
}

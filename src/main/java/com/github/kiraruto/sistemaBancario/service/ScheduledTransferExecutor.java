package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.ScheduledTransferRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Executa uma transferência agendada em transação própria.
 *
 * O débito, o crédito, o registro da transação e a mudança de status da
 * transferência agendada precisam ser atômicos: se qualquer etapa falhar,
 * nada pode ser persistido.
 */
@Service
@RequiredArgsConstructor
public class ScheduledTransferExecutor {

    private final SavingsAccountRepository savingsAccountRepository;
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduledTransfer execute(ScheduledTransfer transfer) {
        UUID sourceId = transfer.getSourceAccount();
        UUID targetId = transfer.getTargetAccount();

        // Bloqueia sempre na mesma ordem para evitar deadlock entre transferências opostas.
        boolean sourceIsFirst = sourceId.compareTo(targetId) <= 0;

        SavingsAccount first = lockAccount(sourceIsFirst ? sourceId : targetId);
        SavingsAccount second = lockAccount(sourceIsFirst ? targetId : sourceId);

        SavingsAccount source = sourceIsFirst ? first : second;
        SavingsAccount target = sourceIsFirst ? second : first;

        if (source.getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        source.setBalance(source.getBalance().subtract(transfer.getAmount()));
        target.setBalance(target.getBalance().add(transfer.getAmount()));

        savingsAccountRepository.save(source);
        savingsAccountRepository.save(target);

        transactionRepository.save(new Transaction(source, target, transfer));

        transfer.setStatus(EnumStatus.CONCLUIDA);
        return scheduledTransferRepository.save(transfer);
    }

    private SavingsAccount lockAccount(UUID id) {
        return savingsAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem ou destino não encontrada."));
    }
}

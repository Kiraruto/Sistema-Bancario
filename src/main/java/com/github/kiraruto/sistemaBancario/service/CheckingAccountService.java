package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.TransactionDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.interfaces.TransactionValidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckingAccountService {

    private final CheckingAccountRepository checkingAccountRepository;
    private final CheckingAccountValidate checkingAccountValidate;
    private final TransactionRepository transactionRespository;
    private final TransactionValidate transactionValidate;
    private final UserRepository userRepository;

    public List<CheckingAccount> getAll() {
        return checkingAccountRepository.findAll();
    }

    public CheckingAccount createAccount(CheckingAccountDTO checkingAccountDTO) {
        checkingAccountValidate.validateCreateAccountChecking(checkingAccountDTO);

        User user = userRepository.findById(checkingAccountDTO.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + checkingAccountDTO.userId()));

        CheckingAccount checkingAccount = new CheckingAccount(checkingAccountDTO);
        checkingAccount.setUser(user);

        checkingAccountRepository.save(checkingAccount);

        Optional<CheckingAccount> chOpt = checkingAccountRepository.findByCpfAndEmail(checkingAccountDTO.cpf(), checkingAccountDTO.email());
        user.setCheckingAccount(chOpt.orElseThrow(() -> new IllegalArgumentException("Não existe conta com este Cpf e este Email")));
        userRepository.save(user);

        return checkingAccount;
    }

    public void depositCheckingAccount(DepositRequestDTO depositRequestDTO) {
        checkingAccountValidate.validateDepositChecking(depositRequestDTO);

        CheckingAccount ch = checkingAccountRepository.findByCpfAndEmail(depositRequestDTO.cpf(), depositRequestDTO.email())
                .orElseThrow(() -> new IllegalArgumentException("Não existe conta com este Cpf e este Email"));

        BigDecimal newBalance = depositRequestDTO.balance().add(ch.getBalance());

        Transaction transaction = new Transaction();
        transaction.setAccountSends(ch.getId());
        transaction.setAccountSends(ch.getId());
        transaction.setTransactionType(EnumTransactionType.DEPOSITO);
        transaction.setAmount(depositRequestDTO.balance());
        transaction.setTransactionDate(LocalDateTime.now().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDateTime());
        transaction.setStatus(transactionValidate.deposit(depositRequestDTO));
        transaction.setOrigin(depositRequestDTO.origin());
        transaction.setDescription(depositRequestDTO.description());

        transactionRespository.save(transaction);

        ch.setBalance(newBalance);
        checkingAccountRepository.save(ch);
    }

    public CheckingAccount getById(UUID id) {
        return checkingAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("O id da transação não foi encontrado"));
    }

    public List<TransactionDTO> getTransactions(UUID uuid) {
        if (!checkingAccountRepository.existsById(uuid)) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        List<Transaction> transactions = transactionRespository.findByAccountSends(uuid);

        return transactions.stream()
                .map(TransactionDTO::new)
                .toList();
    }

    public BalanceDTO getBalance(UUID uuid) {
        BalanceDTO fullNameAndBalanceById = checkingAccountRepository.findFullNameAndBalanceById(uuid);

        if (!fullNameAndBalanceById.isActive()) {
            throw new IllegalArgumentException("A conta com este id não está ativa");
        }

        return fullNameAndBalanceById;
    }
}

package com.github.kiraruto.sistemaBancario.utils.interfaces;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;

public interface TransactionValidate {

    EnumStatus deposit(DepositRequestDTO depositRequestDTO);

    EnumOrigin originDeposit(DepositRequestDTO depositRequestDTO);

    EnumOrigin originDepositTransiction(WithdrawRequestDTO withdrawRequestDTO);
}

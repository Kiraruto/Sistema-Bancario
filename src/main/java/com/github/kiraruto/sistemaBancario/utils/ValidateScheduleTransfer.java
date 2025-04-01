package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.repository.ScheduledTransferRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateScheduleTransfer {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final UserRepository userRepository;

//    public void validateTransfer(ScheduledTransferDTO dto) {
//        ScheduledTransfer sc1 = scheduledTransferRepository.findById(dto.accountSends())
//                .orElseThrow(() -> new IllegalArgumentException("O id da conta que está enviando não existe"));
//
//        ScheduledTransfer sc2 = scheduledTransferRepository.findById(dto.accountReceive())
//                .orElseThrow(() -> new IllegalArgumentException("O id da conta que está recebendo não existe"));
//
//        if (userRepository.existsById(dto.))
//
//    }
}

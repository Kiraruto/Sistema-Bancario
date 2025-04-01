package com.github.kiraruto.sistemaBancario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BankHoursService {

    private final HolidayService holidayService;

    public boolean isTedTimeValid(LocalDateTime dataHora) {
        return !isWeekend(dataHora) &&
                !isNationalHoliday(dataHora);
    }

    private boolean isWeekend(LocalDateTime dataHora) {
        DayOfWeek dia = dataHora.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private boolean isNationalHoliday(LocalDateTime dataHora) {
        return holidayService.isFeriado(dataHora.toLocalDate());
    }
}

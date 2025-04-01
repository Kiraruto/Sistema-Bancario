package com.github.kiraruto.sistemaBancario.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    public boolean isFeriado(LocalDate data) {
        return getFixedHolidays(data.getYear()).contains(data) ||
                getHolidaysFurniture(data.getYear()).contains(data);
    }

    private List<LocalDate> getFixedHolidays(int ano) {
        return List.of(
                LocalDate.of(ano, 1, 1),   // Ano Novo
                LocalDate.of(ano, 4, 21),  // Tiradentes
                LocalDate.of(ano, 5, 1),   // Dia do Trabalho
                LocalDate.of(ano, 9, 7),   // Independência
                LocalDate.of(ano, 10, 12), // Nossa Senhora Aparecida
                LocalDate.of(ano, 11, 2),  // Finados
                LocalDate.of(ano, 11, 15), // Proclamação da República
                LocalDate.of(ano, 12, 25)  // Natal
        );
    }

    private List<LocalDate> getHolidaysFurniture(int ano) {
        LocalDate pascoa = calculateEaster(ano);
        return List.of(
                pascoa.minusDays(48),     // Carnaval
                pascoa.minusDays(47),     // Carnaval (emenda de feriado)
                pascoa.minusDays(2),      // Sexta-feira Santa
                pascoa,                   // Páscoa
                pascoa.plusDays(60),     // Corpus Christi
                pascoa.plusDays(60).plusDays(1) // Emenda de Corpus Christi
        );
    }

    private LocalDate calculateEaster(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}

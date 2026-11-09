package com.github.kiraruto.sistemaBancario.utils;

import java.math.BigDecimal;

public final class BankLimits {

     public static final BigDecimal LIMIT_SAKE_24H = BigDecimal.valueOf(2000);
     public static final BigDecimal LIMIT_SAKE_PENDING = BigDecimal.valueOf(20000);

    private BankLimits() {
    }
}

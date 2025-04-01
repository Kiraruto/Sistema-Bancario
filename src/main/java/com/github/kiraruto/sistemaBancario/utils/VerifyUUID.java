package com.github.kiraruto.sistemaBancario.utils;

import java.util.UUID;

public class VerifyUUID {

    public static UUID validateUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID inválido: " + id);
        }
    }
}

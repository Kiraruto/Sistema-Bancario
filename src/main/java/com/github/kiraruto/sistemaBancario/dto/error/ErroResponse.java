package com.github.kiraruto.sistemaBancario.dto.error;

import org.springframework.http.HttpStatus;

import java.util.List;

public record ErroResponse(int status, String message, List<ErrorCamp> erros) {

    // Método para erro 400 (Bad Request)
    public static ErroResponse badRequest(String message) {
        return new ErroResponse(HttpStatus.BAD_REQUEST.value(), message, List.of());
    }

    //método para erro 401 (Unauthorized Error)
    public static ErroResponse unauthorized(String message) {
        return new ErroResponse(HttpStatus.UNAUTHORIZED.value(), message, List.of());
    }

    //método para erro 403 (Forbidden Error)
    public static ErroResponse forbidden(String message) {
        return new ErroResponse(HttpStatus.FORBIDDEN.value(), message, List.of());
    }

    //método para erro 404 (Not Found Error)
    public static ErroResponse notFound(String message) {
        return new ErroResponse(HttpStatus.NOT_FOUND.value(), message, List.of());
    }

    //método para erro 405 (Method Not Allowed Error)
    public static ErroResponse methodNotAllowed(String message) {
        return new ErroResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), message, List.of());
    }

    // Método para erro 409 (Conflict)
    public static ErroResponse conflict(String message) {
        return new ErroResponse(HttpStatus.CONFLICT.value(), message, List.of());
    }

    //método para erro 422 (Unprocessable Entity)
    public static ErroResponse unprocessable(String message) {
        return new ErroResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), message, List.of());
    }

    //método para erro 500 (Internal Server Error)
    public static ErroResponse internalServerError(String message) {
        return new ErroResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, List.of());
    }
}

package com.github.kiraruto.sistemaBancario.global.exception;

import com.github.kiraruto.sistemaBancario.dto.error.ErroResponse;
import com.github.kiraruto.sistemaBancario.dto.error.ErrorCamp;
import com.github.kiraruto.sistemaBancario.exceptions.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Erro interno do servidor.";

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, Object>> handleExpiredJwtException(ExpiredJwtException ex, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("error", "Unauthorized");
        response.put("message", "Your session has expired. Please log in again.");
        response.put("path", request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getFieldErrors();
        List<ErrorCamp> listErrors = fieldErrors.stream().map(f -> new ErrorCamp(
                        f.getField(),
                        f.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ErroResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Erro de validação", listErrors);
    }

    @ExceptionHandler(TypeTransactionInvalidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleTypeTransactionInvalidException(RuntimeException e) {
        return ErroResponse.unprocessable(e.getMessage());
    }

    /**
     * Erros de leitura do corpo e de conversão de parâmetros não devem expor
     * detalhes do Jackson/Hibernate. A mensagem devolvida é genérica.
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse handleMalformedRequest(RuntimeException e) {
        log.debug("Requisição malformada: {}", e.getMessage());
        return ErroResponse.badRequest("Requisição inválida.");
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            AccountNotFoundException.class,
            DocLimitExceededException.class,
            ScheduleDocClosedException.class,
            InsufficientBalanceException.class,
            BankingHoursException.class,
            FraudDetectedException.class,
            ConstraintViolationException.class,
            InvalidAccountIdException.class,
            InvalidCpfException.class,
            InvalidAmountException.class,
            FrequentLargeDepositsException.class,
            DepositLimitExceededException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse handleIllegalArgumentException(RuntimeException e) {
        return ErroResponse.badRequest(e.getMessage());
    }

    @ExceptionHandler(RegisterDuplicateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse handleRegisterDuplicateException(RuntimeException e) {
        return ErroResponse.conflict(e.getMessage());
    }

    /**
     * Violação de integridade costuma carregar SQL e nomes de constraints.
     * O cliente recebe apenas uma mensagem genérica.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("Violação de integridade ao processar a requisição", e);
        return ErroResponse.conflict("Conflito ao processar a requisição.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse handleAuthenticationException(RuntimeException e) {
        return ErroResponse.unauthorized(e.getMessage());
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            UsernameNotFoundException.class
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErroResponse handleAccessDeniedException(RuntimeException e) {
        return ErroResponse.forbidden(e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErroResponse handleHttpRequestMethodNotSupportedException(RuntimeException e) {
        return ErroResponse.methodNotAllowed(e.getMessage());
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            EntityNotFoundException.class,
            PendingTransactionNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse handleNoHandlerFoundException(RuntimeException e) {
        return ErroResponse.notFound(e.getMessage());
    }

    /**
     * Último recurso: nunca devolve a mensagem interna (pode conter SQL, paths
     * ou detalhes de implementação). O detalhe fica apenas no log.
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResponse handleUnexpected(Throwable e) {
        log.error("Erro inesperado ao processar a requisição", e);
        return ErroResponse.internalServerError(INTERNAL_ERROR_MESSAGE);
    }
}

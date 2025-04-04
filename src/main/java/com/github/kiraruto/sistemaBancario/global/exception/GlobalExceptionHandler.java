package com.github.kiraruto.sistemaBancario.global.exception;

import com.github.kiraruto.sistemaBancario.dto.error.ErroResponse;
import com.github.kiraruto.sistemaBancario.dto.error.ErrorCamp;
import com.github.kiraruto.sistemaBancario.exceptions.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler({
            NullPointerException.class,
            StackOverflowError.class,
            Exception.class,
            IllegalStateException.class

})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResponse handleNullPointerException(RuntimeException e) {
        return ErroResponse.internalServerError(e.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            AccountNotFoundException.class,
            DocLimitExceededException.class,
            ScheduleDocClosedException.class,
            HttpMessageNotReadableException.class,
            InsufficientBalanceException.class,
            BankingHoursException.class,
            FraudDetectedException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            InvalidAccountIdException.class,
            InvalidCpfException.class,
            InvalidAmountException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse handleIllegalArgumentException(RuntimeException e) {
        return ErroResponse.badRequest(e.getMessage());
    }

    @ExceptionHandler({
            RegisterDuplicateException.class,
            DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse handleRegisterDuplicateException(RuntimeException e) {
        return ErroResponse.conflict(e.getMessage());
    }

    @ExceptionHandler({
            AuthenticationException.class
    })
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
            EntityNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse handleNoHandlerFoundException(RuntimeException e) {
        return ErroResponse.notFound(e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResponse handleUnexpected(Throwable e) {
        return ErroResponse.internalServerError("Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }
}

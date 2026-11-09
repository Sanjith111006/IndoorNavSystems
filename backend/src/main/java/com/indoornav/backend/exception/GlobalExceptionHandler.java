package com.indoornav.backend.exception;

import com.indoornav.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converts every failure in the MVC layer to the error body in API.md Section 4.2. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Duration SERVICE_RETRY_AFTER = Duration.ofSeconds(5);

    private final ErrorResponseFactory errorResponseFactory;

    public GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(LoginThrottledException.class)
    ResponseEntity<ErrorResponse> handleThrottled(
            LoginThrottledException exception, HttpServletRequest request) {
        return respondWithRetryAfter(ErrorCode.TOO_MANY_REQUESTS, exception.retryAfter(), request);
    }

    @ExceptionHandler(FieldValidationException.class)
    ResponseEntity<ErrorResponse> handleFieldValidation(
            FieldValidationException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldProblem> details =
                List.of(new ErrorResponse.FieldProblem(exception.field(), exception.getMessage()));
        return validationFailed(details, request);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException exception, HttpServletRequest request) {
        return respond(exception.errorCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldProblem> details = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldProblem)
                .toList();
        return validationFailed(details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleParameterValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldProblem> details = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> addProblems(result, details));
        return validationFailed(details, request);
    }

    /**
     * A request body arrives here as {@link ParameterErrors} when the same method also validates
     * a path or query value, and its problems must still be named by the field inside the body.
     */
    private static void addProblems(ParameterValidationResult result, List<ErrorResponse.FieldProblem> details) {
        if (result instanceof ParameterErrors bodyErrors) {
            bodyErrors.getFieldErrors().forEach(error -> details.add(toFieldProblem(error)));
            return;
        }
        String parameterName = result.getMethodParameter().getParameterName();
        result.getResolvableErrors().forEach(
                error -> details.add(new ErrorResponse.FieldProblem(parameterName, error.getDefaultMessage())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        List<ErrorResponse.FieldProblem> details = List.of(
                new ErrorResponse.FieldProblem(exception.getParameterName(), "must be provided"));
        return validationFailed(details, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleMalformed(Exception exception, HttpServletRequest request) {
        log.warn("Rejected malformed request");
        return respond(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.defaultMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.defaultMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.defaultMessage(), request);
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            Exception exception, HttpServletRequest request) {
        return respond(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException exception, HttpServletRequest request) {
        return respond(ErrorCode.CONFLICT, "This item was changed by someone else.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database constraint rejected a write");
        return respond(ErrorCode.CONFLICT, ErrorCode.CONFLICT.defaultMessage(), request);
    }

    @ExceptionHandler({
        CannotCreateTransactionException.class,
        DataAccessResourceFailureException.class,
        QueryTimeoutException.class
    })
    ResponseEntity<ErrorResponse> handleDatabaseUnavailable(
            Exception exception, HttpServletRequest request) {
        log.error("Database unavailable", exception);
        return respondWithRetryAfter(ErrorCode.SERVICE_UNAVAILABLE, SERVICE_RETRY_AFTER, request);
    }

    /** The client hung up, so there is nobody to send an error body to. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void handleClientDisconnected(AsyncRequestNotUsableException exception) {
        log.debug("Client disconnected before the response was written");
    }

    /** The single global boundary (AGENTS.md 10.1): details go to the log, never to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected failure", exception);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), request);
    }

    private static ErrorResponse.FieldProblem toFieldProblem(FieldError error) {
        return new ErrorResponse.FieldProblem(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> validationFailed(
            List<ErrorResponse.FieldProblem> details, HttpServletRequest request) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.status())
                .body(errorResponseFactory.create(code, code.defaultMessage(), request, details));
    }

    private ResponseEntity<ErrorResponse> respond(
            ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.status())
                .body(errorResponseFactory.create(code, message, request, null));
    }

    private ResponseEntity<ErrorResponse> respondWithRetryAfter(
            ErrorCode code, Duration retryAfter, HttpServletRequest request) {
        return ResponseEntity.status(code.status())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter.toSeconds()))
                .body(errorResponseFactory.create(code, request));
    }
}

package com.challenge.transfers.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Covers all Spring MVC internal exceptions (400, 404, 405, 415, etc.)
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    String path = ((ServletWebRequest) request).getRequest().getRequestURI();
    log.warn("Framework exception [path={}, status={}, error={}]", path, status.value(), ex.getMessage());

    String code = "ERR-" + status.value();
    ErrorDetail detail = new ErrorDetail("ER-" + status.value(), ex.getMessage(), path);
    ErrorResponse error = new ErrorResponse(code, uuid(), ex.getMessage(), List.of(detail));

    return ResponseEntity.status(status).headers(headers).body(error);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    log.warn("Constraint violation [path={}, error={}]", request.getRequestURI(), ex.getMessage());

    List<ErrorDetail> errors =
        ex.getConstraintViolations().stream()
            .map(cv -> new ErrorDetail("ER-400", cv.getMessage(), request.getRequestURI()))
            .toList();

    return ResponseEntity.badRequest()
        .body(new ErrorResponse("ERR-400", uuid(), "Validation failed", errors));
  }

  @ExceptionHandler(SoapTimeoutException.class)
  public ResponseEntity<ErrorResponse> handleSoapTimeout(
      SoapTimeoutException ex, HttpServletRequest request) {

    log.error(
        "SOAP backend timeout [path={}, error={}]", request.getRequestURI(), ex.getMessage(), ex);

    ErrorDetail detail =
        new ErrorDetail("ER-504", "Upstream service did not respond in time", request.getRequestURI());

    return ResponseEntity.status(504)
        .body(new ErrorResponse("ERR-504", uuid(), "Gateway timeout", List.of(detail)));
  }

  @ExceptionHandler(SoapGatewayException.class)
  public ResponseEntity<ErrorResponse> handleSoapGateway(
      SoapGatewayException ex, HttpServletRequest request) {

    log.error(
        "SOAP gateway error [path={}, error={}]", request.getRequestURI(), ex.getMessage(), ex);

    ErrorDetail detail = new ErrorDetail("ER-502", "Upstream service error", request.getRequestURI());

    return ResponseEntity.status(502)
        .body(new ErrorResponse("ERR-502", uuid(), "Bad gateway", List.of(detail)));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {

    // Full stack trace intentionally logged here — this is an unexpected server error.
    log.error("Unexpected error [path={}, error={}]", request.getRequestURI(), ex.getMessage(), ex);

    ErrorDetail detail =
        new ErrorDetail("ER-500", "An unexpected error occurred", request.getRequestURI());

    return ResponseEntity.internalServerError()
        .body(new ErrorResponse("ERR-500", uuid(), "Internal server error", List.of(detail)));
  }

  private static String uuid() {
    return UUID.randomUUID().toString();
  }
}

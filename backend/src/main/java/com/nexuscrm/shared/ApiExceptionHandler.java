package com.nexuscrm.shared;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.servlet.NoHandlerFoundException.class
  })
  ResponseEntity<ProblemDetail> missing(Exception error) {
    return response(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ProblemDetail> method(Exception error) {
    return response(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido neste recurso.");
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ProblemDetail> media(Exception error) {
    return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado.");
  }

  @ExceptionHandler({
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class
  })
  ResponseEntity<ProblemDetail> missingInput(Exception error) {
    return response(HttpStatus.BAD_REQUEST, "Há um parâmetro ou arquivo obrigatório ausente.");
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException error) {
    return response(error.getStatus(), error.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException error) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os campos informados.");
    Map<String, String> fields = new LinkedHashMap<>();
    error
        .getBindingResult()
        .getFieldErrors()
        .forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
    problem.setProperty("errors", fields);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ProblemDetail> malformed(Exception error) {
    return response(HttpStatus.BAD_REQUEST, "Os dados ou filtros informados são inválidos.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> integrity(Exception error) {
    return response(
        HttpStatus.CONFLICT,
        "Registro duplicado ou vínculo inválido. Confira os dados e a organização.");
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> stale(Exception error) {
    return response(
        HttpStatus.CONFLICT,
        "O registro foi alterado por outra pessoa. Atualize a página antes de salvar.");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ProblemDetail> upload(Exception error) {
    return response(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo excede o limite permitido de 10 MB.");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception error) {
    String reference = java.util.UUID.randomUUID().toString();
    LOG.error("Request failed reference={} type={}", reference, error.getClass().getSimpleName());
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Não foi possível concluir a operação. Referência: " + reference);
  }

  private ResponseEntity<ProblemDetail> response(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}

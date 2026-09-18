package com.schwab.assignment;

import com.schwab.assignment.url.UrlService.UrlNotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {
  record ApiError(String message) {}
  @ExceptionHandler({UrlNotFoundException.class, NoSuchElementException.class})
  ResponseEntity<ApiError> notFound(RuntimeException exception) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage())); }
  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  ResponseEntity<ApiError> badRequest(RuntimeException exception) { return ResponseEntity.badRequest().body(new ApiError(exception.getMessage())); }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) { return ResponseEntity.badRequest().body(new ApiError("request validation failed")); }
}

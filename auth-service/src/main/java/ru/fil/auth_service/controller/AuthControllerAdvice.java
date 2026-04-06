package ru.fil.auth_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.fil.auth_service.dto.SimpleErrorMessageResponse;

@RestControllerAdvice
public class AuthControllerAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleErrorMessageResponse> handleException(Exception e) {
        return ResponseEntity.badRequest().body(new SimpleErrorMessageResponse(e.getMessage()));
    }
}

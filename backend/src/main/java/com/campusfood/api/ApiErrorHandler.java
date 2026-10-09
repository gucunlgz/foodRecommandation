package com.campusfood.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {
    private Map<String, Object> body(String code, String message) {
        return Map.of("success", false, "code", code, "message", message,
            "timestamp", OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> api(ApiException e) {
        return ResponseEntity.status(e.status).body(body(e.code, e.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception e) {
        return ResponseEntity.badRequest().body(body("INVALID_INPUT", "输入格式有误，请检查价格、距离或评分。"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        return ResponseEntity.internalServerError().body(body("SERVER_ERROR", "服务暂时不可用，请稍后重试。"));
    }
}

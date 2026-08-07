package com.example.Aimaca.Common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    //业务逻辑错误处理
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBussinessException(BusinessException e) {
        log.warn("业务处理错误");
        return Result.error(e.getCode(), e.getMessage());
    }

    //@Valid校验RequestBody失败处理
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.warn("RequestBody异常",e);
        return Result.error(400, msg);
    }

    //@RequestParam校验失败
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors()
                .stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.warn("@RequestParam校验异常",e);
        return Result.error(400, msg);
    }
    //JSON解析失败
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON",e);
        return Result.error(400,"请求参数格式错误");
    }
    //兜底，其余的未捕获异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.warn("未捕获异常",e);
        return Result.error(500,"土豆炸啦！？");
    }
}

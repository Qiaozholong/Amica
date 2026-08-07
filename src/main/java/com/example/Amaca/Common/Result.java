package com.example.Amaca.Common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    private Result() {
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<T>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<T>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        Result<T> r = new Result<T>();
        r.message = message;
        r.code = 401;
        r.data = null;
        return r;
    }
}
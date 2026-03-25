package com.Baluga;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data){
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static Result error(String msg){
        Result r = new Result();
        r.setCode(500);
        r.setMessage(msg);
        return r;
    }
}

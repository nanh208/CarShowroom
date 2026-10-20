package com.ares.dto;

public class ApiResponse {
    private boolean success;
    private String message;
    private String code;
    private Object data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, String code) {
        this.success = success;
        this.message = message;
        this.code = code;
    }

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }

    public static ApiResponse ok(String message, String code) {
        return new ApiResponse(true, message, code);
    }

    public static ApiResponse ok(String message, Object data) {
        ApiResponse res = new ApiResponse(true, message);
        res.setData(data);
        return res;
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

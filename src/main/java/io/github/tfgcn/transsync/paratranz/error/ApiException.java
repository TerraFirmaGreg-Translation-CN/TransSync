package io.github.tfgcn.transsync.paratranz.error;

import lombok.Getter;

public class ApiException extends RuntimeException {
    @Getter
    private final Integer code;

    public ApiException(String message, Integer code) {
        super(message);
        this.code = code;
    }
}

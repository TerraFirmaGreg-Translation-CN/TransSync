package io.github.transsync.exception;

public class ConfigLoadException extends RuntimeException {
    public ConfigLoadException(String message) {
        super(message);
    }
    public ConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
    public ConfigLoadException(Throwable cause) {
        super(cause);
    }
}
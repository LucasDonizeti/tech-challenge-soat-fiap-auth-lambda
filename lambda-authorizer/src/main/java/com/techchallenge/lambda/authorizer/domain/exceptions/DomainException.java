package com.techchallenge.lambda.authorizer.domain.exceptions;

public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String message) {
        this(message, null);
    }

    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : this.getClass().getSimpleName();
    }

    public String getErrorCode() {
        return errorCode;
    }
}

